package com.onmoim.server.oauth.service.impl;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.onmoim.server.oauth.service.RefreshTokenService;
import com.onmoim.server.oauth.token.TokenProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

	private final RedisTemplate<String, String> redisTemplate;
	private final TokenProperties tokenProperties;

	@Override
	public void saveRefreshToken(Long userId, String refreshToken) {
		redisTemplate.opsForValue().set(
			getKey(userId),
			refreshToken,
			Duration.ofMillis(tokenProperties.getRefreshExpirationTime())
		);
	}

	private String getKey(Long userId) {
		return "refresh:" + userId;
	}

	@Override
	public String getRefreshToken(Long userId) {
		return redisTemplate.opsForValue().get(getKey(userId));
	}

	public void deleteRefreshToken(Long userId) {
		redisTemplate.delete("refresh:" + userId);
	}

}
