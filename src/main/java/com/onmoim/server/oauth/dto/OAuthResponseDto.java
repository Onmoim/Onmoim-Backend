package com.onmoim.server.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthResponseDto {

	private String accessToken;

	private String refreshToken;

	private String status;

}
