package com.onmoim.server.security;

public class JwtHolder {

	private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();

	public static void set(String token) {
		tokenHolder.set(token);
	}

	public static String get() {
		return tokenHolder.get();
	}

	public static void clear() {
		tokenHolder.remove();
	}

}
