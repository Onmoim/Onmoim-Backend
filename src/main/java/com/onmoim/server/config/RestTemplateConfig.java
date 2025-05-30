package com.onmoim.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
	/**
	 * 설정 관련 고도화 예정(ex: 재시도 인터셉터)
	 */
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
