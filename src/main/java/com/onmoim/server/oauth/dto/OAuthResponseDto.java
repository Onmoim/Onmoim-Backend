package com.onmoim.server.oauth.dto;

import com.onmoim.server.oauth.enumeration.SignupStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthResponseDto {

	private String accessToken;

	private String refreshToken;

	private SignupStatus status;

}
