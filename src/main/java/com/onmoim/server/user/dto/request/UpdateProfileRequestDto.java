package com.onmoim.server.user.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "프로필 편집 요청 Request")
public class UpdateProfileRequestDto {

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

	@Schema(description = "자기소개")
	private String introduction;

	@Schema(description = "관심사(카테고리) id 리스트")
	private List<Long> categoryIdList;

	@Schema(description = "프로필 사진 url(기존 사진 교체 없는 경우)")
	private String profileImgUrl;

}
