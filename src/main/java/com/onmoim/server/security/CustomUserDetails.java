package com.onmoim.server.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

	private final Long userId;
	private final String email;
	private final String provider;

	public CustomUserDetails(Long userId, String email, String provider) {
		this.userId = userId;
		this.email = email;
		this.provider = provider;
	}

	public Long getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getProvider() {
		return provider;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(); // 필요하면 ROLE 붙이기
	}

	@Override public String getPassword() {
		return null;
	}

	@Override public String getUsername() {
		return email;
	} // or userId.toString()

	@Override public boolean isAccountNonExpired() {
		return true;
	}

	@Override public boolean isAccountNonLocked() {
		return true;
	}

	@Override public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override public boolean isEnabled() {
		return true;
	}

}
