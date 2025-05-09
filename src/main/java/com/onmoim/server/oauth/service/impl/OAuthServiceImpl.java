package com.onmoim.server.oauth.service.impl;

import java.util.Collections;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.onmoim.server.oauth.dto.OAuthResponse;
import com.onmoim.server.oauth.dto.OAuthUser;
import com.onmoim.server.oauth.service.OAuthService;
import com.onmoim.server.oauth.service.RefreshTokenService;
import com.onmoim.server.oauth.service.provider.GoogleOAuthProvider;
import com.onmoim.server.oauth.service.provider.OAuthProvider;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.security.JwtProvider;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthServiceImpl implements OAuthService {

	private final GoogleOAuthProvider googleProvider;
	// private final KakaoOAuthProvider kakaoProvider;
	private final UserRepository userRepository;
	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;

	@Override
	public OAuthResponse login(String providerName, String token) {

		OAuthProvider provider = switch (providerName.toLowerCase()) {
			case "google" -> googleProvider;
			// case "kakao" -> kakaoProvider;
			default -> throw new IllegalArgumentException("지원하지 않는 provider: " + providerName);
		};

		OAuthUser oAuthUser = provider.getUserInfo(token);

		Optional<User> optionalUser = userRepository.findByOauthIdAndProvider(oAuthUser.oauthId(), providerName);

		if (optionalUser.isPresent()) {
			User user = optionalUser.get();

			CustomUserDetails userDetails = new CustomUserDetails(
				user.getId(),
				user.getEmail(),
				user.getProvider()
			);

			Authentication authentication = new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities()
			);

			// SecurityContextHolder에 인증 객체 심기
			SecurityContextHolder.getContext().setAuthentication(authentication);

			String accessToken = jwtProvider.createAccessToken(authentication);
			String refreshToken = jwtProvider.createRefreshToken(authentication);

			refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

			if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
				log.info("현재 로그인된 사용자 ID: {}", userDetails.getUserId());
			}

			return new OAuthResponse(accessToken, refreshToken, "EXISTS");
		} else {
			return new OAuthResponse(null, null, "NOT_EXISTS");
		}
	}


	@Override
	public OAuthResponse reissueAccessToken(String refreshToken) {
		if (!jwtProvider.validateToken(refreshToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}

		String userId = jwtProvider.getSubject(refreshToken);  // JWT subject (userId)

		String savedToken = refreshTokenService.getRefreshToken(Long.parseLong(userId));

		if (!refreshToken.equals(savedToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "저장된 refresh token과 일치하지 않습니다.");
		}

		// accessToken 재발급
		Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
		String newAccessToken = jwtProvider.createAccessToken(authentication);

		return new OAuthResponse(newAccessToken, refreshToken, "EXISTS");
	}

}
