package com.onmoim.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	@Profile("!test")
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
			.authorizeHttpRequests(authorizeRequests -> authorizeRequests
			.requestMatchers("/test-google-login.html", "/test-kakao-login.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
			.requestMatchers("/auth/**").permitAll()
			.requestMatchers("/api/v1/location", "api/v1/category").permitAll() // 가입 시 필요하므로 제외
			.requestMatchers("/api-docs/**", "/swagger-ui/**", "swagger-resources/**").permitAll()
			.anyRequest().authenticated()
			)

			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(csrf -> csrf.disable())
			.httpBasic(httpBasic -> httpBasic.disable())
			.formLogin(formLogin -> formLogin.disable())
			.sessionManagement(sessionManagement -> sessionManagement
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			);

		return http.build();
	}


	/**
	 * cors 설정
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowCredentials(true);
		configuration.addAllowedOriginPattern("*");
		configuration.addAllowedHeader("*");
		configuration.addAllowedMethod("*");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	@Bean
	@Profile("test")
	public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(
				auth -> auth
					.requestMatchers("/test-google-login.html", "/css/**", "/js/**",
						"/images/**", "/favicon.ico").permitAll()
					.requestMatchers("/auth/**").permitAll()
					.requestMatchers("/api-docs/**", "/swagger-ui/**", "swagger-resources/**").permitAll()
					.anyRequest().authenticated())
			.build();
	}
}
