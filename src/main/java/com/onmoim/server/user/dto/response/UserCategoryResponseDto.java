package com.onmoim.server.user.dto.response;

import com.onmoim.server.user.entity.UserCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCategoryResponseDto {

	private Long categoryId;
	private String categoryName;

	public UserCategoryResponseDto(UserCategory userCategory) {
		this.categoryId = userCategory.getCategory().getId();
		this.categoryName = userCategory.getCategory().getName();
	}

}
