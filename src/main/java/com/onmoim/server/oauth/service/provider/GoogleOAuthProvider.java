package com.onmoim.server.oauth.service.provider;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.onmoim.server.oauth.dto.OAuthUserDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GoogleOAuthProvider implements OAuthProvider {

	@Override
	public OAuthUserDto getUserInfo(String idToken) {
		String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
		Map<?, ?> body = new RestTemplate().getForObject(url, Map.class);

		String sub = (String) body.get("sub");
		String email = (String) body.get("email");

		log.info("sub = {}", sub);
		log.info("email = {}", email);

		return new OAuthUserDto(sub, email);
	}

}
