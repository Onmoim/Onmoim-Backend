package com.onmoim.server.oauth.service.impl;

import static com.onmoim.server.common.exception.ErrorCode.*;
import static com.onmoim.server.oauth.enumeration.SignupStatus.*;

import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.oauth.dto.OAuthResponseDto;
import com.onmoim.server.oauth.dto.OAuthUserDto;
import com.onmoim.server.oauth.service.OAuthService;
import com.onmoim.server.oauth.service.RefreshTokenService;
import com.onmoim.server.oauth.service.provider.GoogleOAuthProvider;
import com.onmoim.server.oauth.service.provider.OAuthProvider;
import com.onmoim.server.oauth.service.provider.OAuthProviderFactory;
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

	private final OAuthProviderFactory oauthProviderFactory;
	private final GoogleOAuthProvider googleProvider;
	// private final KakaoOAuthProvider kakaoProvider;
	private final UserRepository userRepository;
	private final JwtProvider jwtProvider;
	private final RefreshTokenService refreshTokenService;

	@Override
	public OAuthResponseDto handleGoogleLogin(String providerName, String authorizationCode) {
		OAuthProvider provider = oauthProviderFactory.getProvider(providerName);
		OAuthUserDto oAuthUserDto = provider.getUserInfoByAuthorizationCode(authorizationCode);

		return processUserLogin(oAuthUserDto, providerName);
	}

	@Override
	public OAuthResponseDto handleKakaoLogin(String providerName, String authorizationCode) {
		OAuthProvider provider = oauthProviderFactory.getProvider(providerName);
		OAuthUserDto oAuthUserDto = provider.getUserInfoByAuthorizationCode(authorizationCode);

		return processUserLogin(oAuthUserDto, providerName);
	}

	private OAuthResponseDto processUserLogin(OAuthUserDto oAuthUserDto, String providerName) {
		Optional<User> optionalUser = userRepository.findByOauthIdAndProvider(oAuthUserDto.getOauthId(), providerName);

		if (optionalUser.isEmpty()) {
			String signupToken = jwtProvider.createSignupToken(oAuthUserDto.getProvider(), oAuthUserDto.getOauthId(),
				oAuthUserDto.getEmail());
			return new OAuthResponseDto(signupToken, null, NOT_EXISTS);
		}

		User user = optionalUser.get();
		Authentication authentication = createAuthentication(user);
		setAuthenticationToContext(authentication);

		String accessToken = jwtProvider.createAccessToken(authentication);
		String refreshToken = jwtProvider.createRefreshToken(authentication);
		refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

		return new OAuthResponseDto(accessToken, refreshToken, EXISTS);
	}

	private Authentication createAuthentication(User user) {
		CustomUserDetails userDetails = new CustomUserDetails(user.getId(), user.getEmail(), user.getProvider());
		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
	}

	private void setAuthenticationToContext(Authentication authentication) {
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}


	@Override
	public OAuthResponseDto reissueAccessToken(String refreshToken) {
		if (!jwtProvider.validateToken(refreshToken)) {
			throw new CustomException(INVALID_REFRESH_TOKEN);
		}

		Long userId = Long.parseLong(jwtProvider.getSubject(refreshToken));  // JWT subject (userId)
		String savedToken = refreshTokenService.getRefreshToken(userId);

		if (!refreshToken.equals(savedToken)) {
			throw new CustomException(REFRESH_TOKEN_MISMATCH);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		Authentication authentication = createAuthentication(user);
		String newAccessToken = jwtProvider.createAccessToken(authentication);

		return new OAuthResponseDto(newAccessToken, refreshToken, EXISTS);
	}


}
