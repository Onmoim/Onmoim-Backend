package com.onmoim.server.oauth.service;

public interface RefreshTokenService {

	void saveRefreshToken(Long userId, String refreshToken);

	String getRefreshToken(Long userId);

	void deleteRefreshToken(Long userId);

}
