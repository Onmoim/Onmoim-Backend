package com.onmoim.server.oauth.service.provider;

import com.onmoim.server.oauth.dto.OAuthUserDto;

public interface OAuthProvider {

	String getProviderName();

	OAuthUserDto getUserInfoByAuthorizationCode(String code);

}
