package com.onmoim.server.oauth.service.impl;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.onmoim.server.oauth.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

	private final RedisTemplate<String, String> redisTemplate;

	private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7일

	@Override
	public void saveRefreshToken(Long userId, String refreshToken) {
		redisTemplate.opsForValue().set(
			getKey(userId),
			refreshToken,
			Duration.ofMillis(REFRESH_TOKEN_EXPIRATION)
		);
	}

	private String getKey(Long userId) {
		return "refresh:" + userId;
	}

	@Override
	public String getRefreshToken(Long userId) {
		return redisTemplate.opsForValue().get(getKey(userId));
	}

}
