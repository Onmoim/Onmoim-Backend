package com.onmoim.server.oauth.service.impl;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.onmoim.server.oauth.service.RefreshTokenService;
import com.onmoim.server.oauth.token.TokenProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

	private final RedisTemplate<String, String> redisTemplate;
	private final TokenProperties tokenProperties;

	@Override
	public void saveRefreshToken(Long userId, String refreshToken) {
		String key = getKey(userId);
		log.info("Redis 저장 시작: key={}, value={}", key, refreshToken);

		redisTemplate.opsForValue().set(
			key,
			refreshToken,
			Duration.ofMillis(tokenProperties.getRefreshExpirationTime())
		);

		log.info("Redis 저장 완료: key={}", key);
	}

	private String getKey(Long userId) {
		return "refresh:" + userId;
	}

	@Override
	public String getRefreshToken(Long userId) {
		return redisTemplate.opsForValue().get(getKey(userId));
	}

}
