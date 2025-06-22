package com.onmoim.server.oauth.service.provider;

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
import org.springframework.web.client.RestTemplate;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.oauth.dto.OAuthUserDto;

import lombok.extern.slf4j.Slf4j;

import static com.onmoim.server.common.exception.ErrorCode.*;
import static com.onmoim.server.oauth.constant.OAuthApiConstants.KAKAO_USER_INFO_URL;

@Component
@Slf4j
public class KakaoOAuthProvider implements OAuthProvider {

	@Value("${spring.security.oauth2.client.registration.kakao.client-id}")
	private String clientId;

	@Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
	private String clientSecret;

	@Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
	private String redirectUri;

	@Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
	private String tokenUri;

	@Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
	private String userInfoUri;

	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public String getProviderName() {
		return "kakao";
	}

	@Override
	public OAuthUserDto getUserInfoByAuthorizationCode(String code) {
		try {
			// 1. Access Token 요청
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("grant_type", "authorization_code");
			params.add("client_id", clientId);
			params.add("client_secret", clientSecret);
			params.add("redirect_uri", redirectUri);
			params.add("code", code);

			HttpEntity<?> request = new HttpEntity<>(params, headers);


			ResponseEntity<Map> response = restTemplate.postForEntity(
				tokenUri,
				request,
				Map.class
			);

			String accessToken = (String) response.getBody().get("access_token");

			// 2. 사용자 정보 요청
			HttpHeaders userHeaders = new HttpHeaders();
			userHeaders.setBearerAuth(accessToken);

			HttpEntity<?> userInfoRequest = new HttpEntity<>(userHeaders);

			ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
				userInfoUri,
				HttpMethod.GET,
				userInfoRequest,
				Map.class
			);

			Map<String, Object> body = userInfoResponse.getBody();
			log.info("body = {}", body);

			Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");
			String email = (String) kakaoAccount.get("email");
			String kakaoId = String.valueOf(body.get("id"));
			log.info("email = {}", email);
			log.info("kakaoId = {}", kakaoId);

			return new OAuthUserDto("kakao", kakaoId, email);
		} catch (Exception e) {
			throw new CustomException(OAUTH_PROVIDER_ERROR);
		}

	}

}
