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
	public OAuthUserDto getUserInfoByToken(String token) {
		try {
			// accessToken으로 사용자 정보 요청
			HttpHeaders userHeaders = new HttpHeaders();
			userHeaders.setBearerAuth(token);

			HttpEntity<?> userInfoRequest = new HttpEntity<>(userHeaders);
			ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
				userInfoUri,
				HttpMethod.GET,
				userInfoRequest,
				Map.class
			);

			log.info("userInfoResponse = {}", userInfoResponse);

			Map<String, Object> userInfo = userInfoResponse.getBody();
			log.info("userInfo = {}", userInfo);

			Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
			String kakaoId = String.valueOf(userInfo.get("id"));
			String email = (String) kakaoAccount.get("email");
			log.info("email = {}", email);
			log.info("kakaoId = {}", kakaoId);

			return new OAuthUserDto("kakao", kakaoId, email);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException(OAUTH_PROVIDER_ERROR);
		}

	}

}
