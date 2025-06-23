package com.onmoim.server.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class OAuthRequestDto {

	@Schema(description = "소셜 프로바이더(google/kakao)")
	@Pattern(regexp = "^(google|kakao)$", message = "provider는 google 또는 kakao만 가능합니다.")
	private String provider;

	@Schema(description = "구글: idToken, 카카오: accessToken")
	private String token;

}
