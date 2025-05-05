package com.onmoim.server.security;

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile("local")
public class StubAuthenticationFilter extends OncePerRequestFilter {

	private final Random random = new Random();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {

		// 테스트용 사용자 정보
		Long fakeUserId = Long.valueOf(random.nextInt(100) + 1);
		Map<String, Object> fakeAttributes = Map.of("mock", "true");

		CustomOAuth2User user = new CustomOAuth2User(fakeUserId, fakeAttributes);

		UsernamePasswordAuthenticationToken authentication =
			new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

		SecurityContextHolder.getContext().setAuthentication(authentication);
		filterChain.doFilter(request, response);
	}

}
