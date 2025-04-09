package com.onmoim.server.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
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