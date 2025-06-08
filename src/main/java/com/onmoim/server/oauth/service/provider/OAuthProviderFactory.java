package com.onmoim.server.oauth.service.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class OAuthProviderFactory {

	private final Map<String, OAuthProvider> providers;

	public OAuthProviderFactory(List<OAuthProvider> providerList) {
		this.providers = providerList.stream()
			.collect(Collectors.toMap(
				provider -> provider.getProviderName().toLowerCase(),
				Function.identity()
			));
	}

	public OAuthProvider getProvider(String providerName) {
		OAuthProvider provider = providers.get(providerName.toLowerCase());
		return provider;
	}

}
