package com.onmoim.server.oauth.service.provider;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.onmoim.server.oauth.dto.OAuthUserDto;

import lombok.extern.slf4j.Slf4j;

import static com.onmoim.server.oauth.constant.OAuthApiConstants.GOOGLE_USER_INFO_URL;

@Component
@Slf4j
public class GoogleOAuthProvider implements OAuthProvider {

	@Override
	public String getProviderName() {
		return "google";
	}

	@Override
	public OAuthUserDto getUserInfo(String idToken) {
		String url = GOOGLE_USER_INFO_URL + idToken;
		Map<?, ?> body = new RestTemplate().getForObject(url, Map.class);

		String sub = (String) body.get("sub");
		String email = (String) body.get("email");

		log.info("sub = {}", sub);
		log.info("email = {}", email);

		return new OAuthUserDto(sub, email);
	}

}
