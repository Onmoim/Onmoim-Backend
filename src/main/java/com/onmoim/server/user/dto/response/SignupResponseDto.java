package com.onmoim.server.user.dto.response;

import com.onmoim.server.oauth.enumeration.SignupStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponseDto {

	private Long userId;

	private String accessToken;

	private String refreshToken;

	private SignupStatus status;

}
