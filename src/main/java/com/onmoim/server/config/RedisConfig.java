package com.onmoim.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port}")
	private int port;

	@Value("${spring.profiles.active}")
	private String activeProfile;

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
		LettuceClientConfiguration clientConfig;

		if (isSslEnabled()) {
			clientConfig = LettuceClientConfiguration.builder()
				.useSsl()
				.build();
		} else {
			clientConfig = LettuceClientConfiguration.builder()
				.build();
		}

		return new LettuceConnectionFactory(config, clientConfig);
	}

	@Bean
	@Primary
	public RedisTemplate<String, String> redisTemplate() {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
		StringRedisSerializer serializer = new StringRedisSerializer();

		redisTemplate.setKeySerializer(serializer);
		redisTemplate.setValueSerializer(serializer);
		redisTemplate.setConnectionFactory(redisConnectionFactory());

		// ElastiCache 충돌 방지용
		redisTemplate.setHashKeySerializer(serializer);
		redisTemplate.setHashValueSerializer(serializer);

		return redisTemplate;
	}

	private boolean isSslEnabled() {
		return "dev".equals(activeProfile); // dev에서만 SSL 사용
	}

}
