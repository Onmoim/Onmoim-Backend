package com.onmoim.server.oauth.service;

import com.onmoim.server.oauth.dto.OAuthResponseDto;

public interface OAuthService {

	OAuthResponseDto login(String provider, String token);

	OAuthResponseDto reissueAccessToken(String refreshToken);

}
