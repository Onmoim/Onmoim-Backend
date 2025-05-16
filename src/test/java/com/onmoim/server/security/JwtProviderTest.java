package com.onmoim.server.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@SpringBootTest
public class JwtProviderTest {

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	void createAccessTokenTest() {
		// given
		Long userId = 1L;
		String email = "test@test.com";
		String provider = "google";

		CustomUserDetails userDetails = new CustomUserDetails(userId, email, provider);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			userDetails,
			null
			, Collections.emptyList()
		);
		// when
		String accessToken = jwtProvider.createAccessToken(authentication);

		// then
		assertNotNull(accessToken);
		assertTrue(jwtProvider.validateToken(accessToken));
		assertEquals(userId.toString(), jwtProvider.getSubject(accessToken));
	}

	@Test
	@Disabled
	void createRefreshTokenTest() {
		// given
		Long userId = 1L;
		String email = "test@test.com";
		String provider = "google";

		CustomUserDetails userDetails = new CustomUserDetails(userId, email, provider);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			userDetails,
			null
			, Collections.emptyList()
		);

		// when
		String refreshToken = jwtProvider.createRefreshToken(authentication);

		// then
		assertNotNull(refreshToken);
		assertTrue(jwtProvider.validateToken(refreshToken));
		assertEquals(userId.toString(), jwtProvider.getSubject(refreshToken));
	}

	@Test
	void invalidTokenReturnFalse() {
		// given
		String invalidToken = "sadasdfasdf123";

		// when
		boolean result = jwtProvider.validateToken(invalidToken);

		// then
		assertFalse(result);
	}

}
