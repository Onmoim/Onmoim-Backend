package com.onmoim.server.category.dto;

import com.onmoim.server.category.entity.Category;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class CategoryResponseDto {

	@Schema(description = "category id")
	private Long categoryId;

	@Schema(description = "이름")
	private String name;

	@Schema(description = "아이콘 url")
	private String iconUrl;

	public CategoryResponseDto(Long categoryId, String name, String iconUrl) {
		this.categoryId = categoryId;
		this.name = name;
		this.iconUrl = iconUrl;
	}

	public static CategoryResponseDto from(Category category) {
		return new CategoryResponseDto(
			category.getId(),
			category.getName(),
			category.getIconUrl()
		);
	}

}
