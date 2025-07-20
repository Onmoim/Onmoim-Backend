package com.onmoim.server.group.dto;


import com.onmoim.server.group.entity.Status;

import jakarta.annotation.Nullable;


public record GroupDetail(
	Long groupId,
	String title,
	String description,
	String address,
	String category,
	String imgUrl,
	@Nullable
	Status status
)
{

}
