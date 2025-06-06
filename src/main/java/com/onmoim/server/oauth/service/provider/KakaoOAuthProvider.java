package com.onmoim.server.oauth.service.provider;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.oauth.dto.OAuthUserDto;

import lombok.extern.slf4j.Slf4j;

import static com.onmoim.server.oauth.constant.OAuthApiConstants.KAKAO_USER_INFO_URL;

@Component
@Slf4j
public class KakaoOAuthProvider implements OAuthProvider {

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public String getProviderName() {
		return "kakao";
	}

	@Override
	public OAuthUserDto getUserInfo(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.set("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

		HttpEntity<Void> request = new HttpEntity<>(headers);

		ResponseEntity<Map> response = restTemplate.exchange(
			KAKAO_USER_INFO_URL,
			HttpMethod.GET,
			request,
			Map.class
		);

		Map<String, Object> body = response.getBody();
		if (body == null || body.get("id") == null) {
			throw new CustomException(ErrorCode.INVALID_KAKAO_RESPONSE);
		}
		log.info("body = {}", body);

		String oauthId = body.get("id").toString();
		// String email = ((Map<String, Object>) body.get("kakao_account")).get("email").toString();
		log.info("oauthId = {}", oauthId);
		// log.info("email = {}", email);

		return new OAuthUserDto(oauthId, null);
	}

}
