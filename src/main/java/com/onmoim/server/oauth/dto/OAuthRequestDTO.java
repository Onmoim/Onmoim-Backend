package com.onmoim.server.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class OAuthRequestDTO {

	@Schema(description = "소셜 프로바이더(google/kakao)")
	@Pattern(regexp = "^(google|kakao)$", message = "provider는 google 또는 kakao만 가능합니다.")
	private String provider;

	@Schema(description = "토큰")
	private String token;

}
