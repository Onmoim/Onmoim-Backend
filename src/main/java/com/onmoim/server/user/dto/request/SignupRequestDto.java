package com.onmoim.server.user.dto.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "유저 회원가입 요청 Request")
public class SignupRequestDto {

	@NotNull(message = "소셜 고유 id는 필수입니다.")
	@Schema(description = "소셜 고유 id(google: sub, kakao: id)")
	private String oauthId;

	@NotNull(message = "소셜 프로바이더는 필수입니다.")
	@Schema(description = "소셜 프로바이더(google/kakao)")
	private String provider;    // google, kakao 등

	@NotNull(message = "이메일은 필수입니다.")
	@Schema(description = "이메일")
	private String email;

	@NotNull(message = "이름은 필수입니다.")
	@Schema(description = "이름")
	private String name;

	@NotNull(message = "성별은 필수입니다.")
	@Schema(description = "성별(M/F)")
	private String gender;

	@NotNull(message = "생년월일은 필수입니다.")
	@Schema(description = "생년월일")
	private LocalDateTime birth;

	@NotNull(message = "지역은 필수입니다.")
	@Schema(description = "지역 id")
	private Long addressId;

}
