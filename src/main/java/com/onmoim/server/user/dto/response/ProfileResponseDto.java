package com.onmoim.server.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "유저 프로필 response")
public class ProfileResponseDto {

	@Schema(description = "유저 id")
	private Long id;

	@Schema(description = "이름")
	private String name;

	@Schema(description = "지역")
	private String location;

	@Schema(description = "생년월일")
	private LocalDateTime birth;

	@Schema(description = "자기소개")
	private String introduction;

	@Schema(description = "카테고리 리스트")
	private List<String> categoryList;

	@Schema(description = "프로필 사진 url")
	private String profileImgUrl;

}
