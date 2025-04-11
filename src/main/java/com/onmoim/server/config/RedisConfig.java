package com.onmoim.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {


	@Value("${spring.data.redis.host:localhost}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;
	
	@Value("${REDIS_SSL:false}")
	private boolean useSsl;

	@Bean
	@Profile({"dev", "prod"}) // 개발 및 운영 환경 (ElastiCache)
	public RedisConnectionFactory redisProductionConnectionFactory() {
		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory();
		lettuceConnectionFactory.setHostName(redisHost);
		lettuceConnectionFactory.setPort(redisPort);
		lettuceConnectionFactory.setUseSsl(true); // AWS ElastiCache는 TLS 사용
		return lettuceConnectionFactory;
	}

	@Bean
	@Profile({"local", "test"}) // 로컬 및 테스트 환경 (Docker)
	public RedisConnectionFactory redisLocalConnectionFactory() {
		LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory();
		lettuceConnectionFactory.setHostName(redisHost);
		lettuceConnectionFactory.setPort(redisPort);
		lettuceConnectionFactory.setUseSsl(useSsl); // 환경 변수 설정에 따름 (기본값: false)
    
		return lettuceConnectionFactory;
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate() {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		
		// 현재 프로필에 따라 적절한 ConnectionFactory 사용
		if (isProductionProfile()) {
			template.setConnectionFactory(redisProductionConnectionFactory());
		} else {
			template.setConnectionFactory(redisLocalConnectionFactory());
		}
		
		// 직렬화 설정
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
		
		return template;
	}
	
	// 현재 활성화된 프로필이 운영/개발 환경인지 확인
	private boolean isProductionProfile() {
		String[] activeProfiles = System.getProperty("spring.profiles.active", "").split(",");
		for (String profile : activeProfiles) {
			if ("prod".equals(profile) || "dev".equals(profile)) {
				return true;
			}
		}
		return false;
	}

}
