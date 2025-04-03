package com.onmoim.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openApi() {
		Info info = new Info()
			.version("v1.0") //버전
			.title("Onmoim API") //이름
			.description("온라인 소규모 모임 API"); //설명
		return new OpenAPI()
			.info(info);
	}
}
