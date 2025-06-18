package com.onmoim.server.oauth.service.provider;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.oauth.dto.OAuthUserDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GoogleOAuthProvider implements OAuthProvider {


	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String clientSecret;

	@Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
	private String redirectUri;

	@Value("${spring.security.oauth2.client.registration.google.token-uri}")
	private String tokenUri;

	@Value("${spring.security.oauth2.client.registration.google.user-info-uri}")
	private String userInfoUri;

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public String getProviderName() {
		return "google";
	}

	@Override
	public OAuthUserDto getUserInfoByAuthorizationCode(String code) {

		try {
			// 1. Authorization Code로 Access Token 요청
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("code", code);
			params.add("client_id", clientId);
			params.add("client_secret", clientSecret);
			params.add("redirect_uri", redirectUri);
			params.add("grant_type", "authorization_code");

			HttpEntity<?> request = new HttpEntity<>(params, headers);

			ResponseEntity<Map> response = restTemplate.postForEntity(
				tokenUri,
				request,
				Map.class
			);

			System.out.println("response = " + response.getStatusCode());
			System.out.println("body = " + response.getBody());

			String accessToken = (String) response.getBody().get("access_token");

			// 2. Access Token으로 사용자 정보 요청
			HttpHeaders userHeaders = new HttpHeaders();
			userHeaders.setBearerAuth(accessToken);

			HttpEntity<?> userInfoRequest = new HttpEntity<>(userHeaders);
			ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
				userInfoUri,
				HttpMethod.GET,
				userInfoRequest,
				Map.class
			);

			Map<String, Object> userInfo = userInfoResponse.getBody();
			return new OAuthUserDto("google", (String) userInfo.get("id"), (String) userInfo.get("email"));
		} catch (Exception e) {
			throw new CustomException(OAUTH_PROVIDER_ERROR);
		}

	}

}
