package com.onmoim.server.user.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "유저 회원가입 요청 Request")
public class SignupRequest {

	@Schema(description = "소셜 고유 id(google: sub, kakao: id)")
	private String oauthId;

	@Schema(description = "소셜 프로바이더(google/kakao)")
	private String provider;    // google, kakao 등

	@Schema(description = "이메일")
	private String email;

	@Schema(description = "이름")
	private String name;

	@Schema(description = "성별(M/F)")
	private String gender;

	@Schema(description = "생년월일")
	private LocalDateTime birth;

	@Schema(description = "지역 id")
	private Long addressId;

	@Schema(description = "관심사(카테고리) id")
	private Long categoryId;

}
