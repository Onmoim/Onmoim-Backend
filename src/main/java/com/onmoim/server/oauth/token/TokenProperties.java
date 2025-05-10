package com.onmoim.server.oauth.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenProperties {

	@Value("${jwt.token.access-expiration-time}")
	private long accessExpirationTime;

	@Value("${jwt.token.refresh-expiration-time}")
	private long refreshExpirationTime;

	public long getAccessExpirationTime() {
		return accessExpirationTime;
	}

	public long getRefreshExpirationTime() {
		return refreshExpirationTime;
	}

}
