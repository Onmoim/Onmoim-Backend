package com.onmoim.server.oauth.service.provider;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.oauth.dto.OAuthUserDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GoogleOAuthProvider implements OAuthProvider {


	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String clientId;

	@Override
	public String getProviderName() {
		return "google";
	}

	public OAuthUserDto getUserInfoByToken(String idTokenString) {

		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), JacksonFactory.getDefaultInstance())
			.setAudience(Collections.singletonList(clientId))
			.build();

		try {
			// 클라이언트에서 받은 idToken을 검증
			GoogleIdToken idToken = verifier.verify(idTokenString);

			if (idToken != null) {
				GoogleIdToken.Payload payload = idToken.getPayload();
				String sub = payload.getSubject();
				String email = payload.getEmail();
				log.info("email = {}", email);

				return new OAuthUserDto("google", sub, email);
			} else {
				throw new CustomException(INVALID_OAUTH_ID_TOKEN);
			}
		} catch (GeneralSecurityException | IOException e) {
			log.error("Google ID token verification failed", e);
			throw new CustomException(OAUTH_PROVIDER_ERROR);
		}
	}

}
