package com.onmoim.server.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@SecurityScheme(
	name = "bearerAuth",
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT"
)
@OpenAPIDefinition(
	security = @SecurityRequirement(name = "bearerAuth")
)
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openApi() {
		Info info = new Info()
			.version("v1.0") //버전
			.title("Onmoim API") //이름
			.description("온라인 소규모 모임 API"); //설명
		return new OpenAPI()
			.servers(Arrays.asList(
				new Server().url("http://localhost:8080").description("Local 서버"),
				new Server().url("https://onmoim.store").description("Production 서버")
			))
			.info(info);
	}

}
