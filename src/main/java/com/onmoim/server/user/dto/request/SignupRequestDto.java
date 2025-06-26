package com.onmoim.server.user.dto.request;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "유저 회원가입 요청 Request")
public class SignupRequestDto {

	@NotNull(message = "이름은 필수입니다.")
	@Schema(description = "이름")
	private String name;

	@NotNull(message = "성별은 필수입니다.")
	@Schema(description = "성별(M/F)")
	private String gender;

	@NotNull(message = "생년월일은 필수입니다.")
	@Schema(description = "생년월일")
	private LocalDate birth;

	@NotNull(message = "지역은 필수입니다.")
	@Schema(description = "지역 id")
	private Long locationId;

}
