package com.onmoim.server.security;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.security.Key;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.oauth.token.TokenProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtProvider {

	@Value("${jwt.secret}")
	private String secretKey;

	private final TokenProperties tokenProperties;

	private Key key;

	@PostConstruct
	public void init() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		key = Keys.hmacShaKeyFor(keyBytes);
	}

	// 회원가입용 Token 생성
	public String createSignupToken(String provider, String oauthId, String email) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + Duration.ofMinutes(10).toMillis()); // 10분 유효

		Claims claims = Jwts.claims();
		claims.put("provider", provider);
		claims.put("oauthId", oauthId);
		claims.put("email", email);
		claims.put("tokenType", "signup");

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(now)
			.setExpiration(expiryDate)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	// Access Token 생성
	public String createAccessToken(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Date now = new Date();
		Date expireDate = new Date(now.getTime() + tokenProperties.getAccessExpirationTime());

		Claims claims = Jwts.claims().setSubject(userDetails.getUserId().toString());

		claims.put("tokenType", "access"); // signup 토큰 발급과 분리

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(now)
			.setExpiration(expireDate)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	// Refresh Token 생성
	public String createRefreshToken(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		Date now = new Date();
		Date expireDate = new Date(now.getTime() + tokenProperties.getRefreshExpirationTime());

		Claims claims = Jwts.claims().setSubject(userDetails.getUserId().toString());

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(now)
			.setExpiration(expireDate)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	// 토큰 검증
	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// Subject 추출
	public String getSubject(String token) {
		return getAllClaims(token).getSubject();
	}

	private Claims getAllClaims(String token) {
		return Jwts.parserBuilder()
			.setSigningKey(key)
			.build()
			.parseClaimsJws(token)
			.getBody();
	}

	public Authentication getAuthentication(String token) {

		Claims claims = getAllClaims(token);
		String tokenType = claims.get("tokenType", String.class);

		// accessToken이 아닐 때 exception
		if (!"access".equals(tokenType)) {
			throw new CustomException(INVALID_ACCESS_TOKEN);
		}

		String subject = getSubject(token);
		return new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
	}

	public Claims parseSignupToken(String token) {
		return Jwts.parser()
			.setSigningKey(secretKey)
			.parseClaimsJws(token)
			.getBody();
	}

	public String getTokenType(String token) {
		try {
			Claims claims = getAllClaims(token);
			return claims.get("tokenType", String.class);
		} catch (Exception e) {
			return null;
		}
	}
}
