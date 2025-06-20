package com.onmoim.server.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
			.setConnectTimeout(Duration.ofSeconds(3)) // 커넥션 타임아웃 3
			.setReadTimeout(Duration.ofSeconds(3))    // 리드 타임아웃 3
			.build();
	}
}
