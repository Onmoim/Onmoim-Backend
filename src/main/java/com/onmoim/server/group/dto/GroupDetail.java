package com.onmoim.server.group.dto;

import com.onmoim.server.category.entity.Category;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.location.entity.Location;

/*
Group - 모임
Category - 카테고리
Location- 위치
 */
public record GroupDetail(
	Long groupId,
	String title,
	String description,
	String address,
	String category
)
{
	public static GroupDetail of(Group group) {
		Category category = group.getCategory();
		Location location = group.getLocation();
		return new GroupDetail(
			group.getId(),
			group.getName(),
			group.getDescription(),
			location.getDong(),
			category.getName()
		);
	}
}
