package com.onmoim.server.security;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtProvider {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.token.access-expiration-time}")
	private long accessExpirationTime;

	@Value("${jwt.token.refresh-expiration-time}")
	private long refreshExpirationTime;

	private Key key;

	@PostConstruct
	public void init() {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		key = Keys.hmacShaKeyFor(keyBytes);
	}

	// Access Token 생성
	public String createAccessToken(Authentication authentication) {
		Date now = new Date();
		Date expireDate = new Date(now.getTime() + accessExpirationTime);

		Claims claims = Jwts.claims().setSubject(authentication.getName()); // ex: userId or email

		return Jwts.builder()
			.setClaims(claims)
			.setIssuedAt(now)
			.setExpiration(expireDate)
			.signWith(key, SignatureAlgorithm.HS256)
			.compact();
	}

	// Refresh Token 생성
	public String createRefreshToken(Authentication authentication) {
		Date now = new Date();
		Date expireDate = new Date(now.getTime() + refreshExpirationTime);

		Claims claims = Jwts.claims().setSubject(authentication.getName());

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
		String subject = getSubject(token);
		return new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());
	}
}
