package com.onmoim.server.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String token = resolveToken(request);

		if (token != null && jwtProvider.validateToken(token)) {

			String tokenType = jwtProvider.getTokenType(token);

			if ("access".equals(tokenType)) { // 기존 accessToken 처리
				Authentication auth = jwtProvider.getAuthentication(token); // SecurityContext에 인증 정보 저장
				SecurityContextHolder.getContext().setAuthentication(auth);
			} else if ("signup".equals(tokenType)) { // signupToken 처리 로직 추가
				 // ThreadLocal에만 저장
				JwtHolder.set(token);
			}

		}

		filterChain.doFilter(request, response);
		JwtHolder.clear();
	}

	private String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		if (bearer != null && bearer.startsWith("Bearer ")) {
			return bearer.substring(7);
		}
		return null;
	}

}
