package com.onmoim.server.user.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "유저 관심사 설정 Request")
public class CreateUserCategoryRequestDto {

	@Schema(description = "유저 id")
	private Long userId;

	@Schema(description = "관심사(카테고리) id 리스트")
	private List<Long> categoryIdList;

}
