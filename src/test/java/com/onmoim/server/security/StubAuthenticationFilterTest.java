package com.onmoim.server.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class StubAuthenticationFilterTest {

	@Test
	@DisplayName("StubAuthenticationFilter userId 반환 테스트")
	void loadFakeUserIdTest() throws ServletException, IOException {

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		StubAuthenticationFilter filter = new StubAuthenticationFilter();

		// 필터 실행
		filter.doFilterInternal(request, response, chain);

		// SecurityContext에 있는 user 확인
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		assertThat(principal).isInstanceOf(CustomOAuth2User.class);

		CustomOAuth2User user = (CustomOAuth2User) principal;
		Long userId = user.getId();
		System.out.println("userId = " + userId);

	}

}
