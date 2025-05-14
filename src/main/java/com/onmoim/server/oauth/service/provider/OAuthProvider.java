package com.onmoim.server.oauth.service.provider;

import com.onmoim.server.oauth.dto.OAuthUserDto;

public interface OAuthProvider {

	OAuthUserDto getUserInfo(String token);

}
