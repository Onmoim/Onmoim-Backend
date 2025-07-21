package com.onmoim.server.group.dto;

import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.Status;

import jakarta.annotation.Nullable;

public record ActiveGroupRelation (
	Long groupId,
	@Nullable
	Long userId,
	@Nullable
	Status status,
	@Nullable
	GroupLikeStatus likeStatus
)
{

}
