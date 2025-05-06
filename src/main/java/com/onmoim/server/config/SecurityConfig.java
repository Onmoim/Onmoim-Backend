package com.onmoim.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.onmoim.server.security.StubAuthenticationFilter;

@Configuration
public class SecurityConfig {

	@Bean
	@Profile("local")
	public SecurityFilterChain localSecurityFilterChain(
			HttpSecurity http, StubAuthenticationFilter stubAuthenticationFilter) throws Exception {
		return http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.addFilterBefore(stubAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

}
