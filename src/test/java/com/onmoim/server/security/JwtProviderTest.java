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
		String userId = "123";
		Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

		// when
		String accessToken = jwtProvider.createAccessToken(authentication);

		// then
		assertNotNull(accessToken);
		assertTrue(jwtProvider.validateToken(accessToken));
		assertEquals(userId, jwtProvider.getSubject(accessToken));
	}

	@Test
	@Disabled
	void createRefreshTokenTest() {
		// given
		String userId = "123";
		Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

		// when
		String refreshToken = jwtProvider.createRefreshToken(authentication);

		// then
		assertNotNull(refreshToken);
		assertTrue(jwtProvider.validateToken(refreshToken));
		assertEquals(userId, jwtProvider.getSubject(refreshToken));
	}

	@Test
	void invalidTokenReturnFalse() {
		// given
		String invalidToken = "RMSidwlsWkdkanfjgrpskcla123";

		// when
		boolean result = jwtProvider.validateToken(invalidToken);

		// then
		assertFalse(result);
	}

}
