package com.onmoim.server.oauth.service.provider;

import com.onmoim.server.oauth.dto.OAuthUser;

public interface OAuthProvider {

	OAuthUser getUserInfo(String token);

}
