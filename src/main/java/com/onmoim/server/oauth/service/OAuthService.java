package com.onmoim.server.oauth.service;

import com.onmoim.server.oauth.dto.OAuthResponseDTO;

public interface OAuthService {

	OAuthResponseDTO login(String provider, String token);

	OAuthResponseDTO reissueAccessToken(String refreshToken);

}
