package com.onmoim.server.oauth.service.provider;

import com.onmoim.server.oauth.dto.OAuthUserDTO;

public interface OAuthProvider {

	OAuthUserDTO getUserInfo(String token);

}
