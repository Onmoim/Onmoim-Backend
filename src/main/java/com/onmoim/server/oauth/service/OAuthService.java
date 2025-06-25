package com.onmoim.server.oauth.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.onmoim.server.oauth.dto.OAuthResponseDto;
import com.onmoim.server.security.CustomUserDetails;
import com.onmoim.server.user.entity.User;

public interface OAuthService {

	OAuthResponseDto handleGoogleLogin(String providerName, String token);

	OAuthResponseDto handleKakaoLogin(String providerName, String token);

	OAuthResponseDto reissueAccessToken(String refreshToken);

	Authentication createAuthentication(User user);

	void setAuthenticationToContext(Authentication authentication);

}
