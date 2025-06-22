package com.onmoim.server.oauth.service;

import com.onmoim.server.oauth.dto.OAuthResponseDto;

public interface OAuthService {

	OAuthResponseDto handleGoogleLogin(String providerName, String authorizationCode);

	OAuthResponseDto handleKakaoLogin(String providerName, String authorizationCode);

	OAuthResponseDto reissueAccessToken(String refreshToken);

}
