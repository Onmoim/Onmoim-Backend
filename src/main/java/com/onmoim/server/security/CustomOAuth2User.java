package com.onmoim.server.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

	private final Long id;
	private final Map<String, Object> attributes;

	public CustomOAuth2User(Long id, Map<String, Object> attributes) {
		this.id = id;
		this.attributes = attributes;
	}

	public Long getId() {
		return id;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Collections.emptyList();
	}

	@Override
	public String getName() {
		return String.valueOf(id);
	}

}
