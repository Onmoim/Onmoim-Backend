package com.onmoim.server.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class OAuthRequestDTO {

	@Schema(description = "소셜 프로바이더(google/kakao)")
	private String provider;

	@Schema(description = "토큰")
	private String token;

}
