package com.onmoim.server.oauth.service;

import com.onmoim.server.oauth.dto.OAuthResponseDto;

public interface OAuthService {

	OAuthResponseDto handleGoogleLogin(String providerName, String token);

	OAuthResponseDto handleKakaoLogin(String providerName, String token);

	OAuthResponseDto reissueAccessToken(String refreshToken);

}
