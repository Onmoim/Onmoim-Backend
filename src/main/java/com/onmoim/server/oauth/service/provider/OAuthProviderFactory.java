package com.onmoim.server.oauth.service.provider;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class OAuthProviderFactory {

	private final Map<String, OAuthProvider> providers;

	public OAuthProviderFactory(GoogleOAuthProvider googleProvider) {
		providers = Map.of("google", googleProvider);
	}

	// public OAuthProviderFactory(
	// 	GoogleOAuthProvider googleProvider,
	// 	KakaoOAuthProvider kakaoProvider
	// ) {
	// 	providers = Map.of(
	// 		"google", googleProvider,
	// 		"kakao", kakaoProvider
	// 	);
	// }

	public OAuthProvider getProvider(String providerName) {
		OAuthProvider provider = providers.get(providerName.toLowerCase());
		return provider;
	}

}
