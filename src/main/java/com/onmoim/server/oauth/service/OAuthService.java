package com.onmoim.server.oauth.service;

import com.onmoim.server.oauth.dto.OAuthResponse;

public interface OAuthService {

	OAuthResponse login(String provider, String token);

	OAuthResponse reissueAccessToken(String refreshToken);

}
