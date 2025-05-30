package com.onmoim.server.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class ReissueTokenRequestDto {

	@Schema(description = "Refresh Token")
	private String refreshToken;

}
