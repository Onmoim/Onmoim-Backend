package com.onmoim.server.group.dto;

import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.Status;

import jakarta.annotation.Nullable;

public record PopularGroupRelation(
	Long groupId,
	@Nullable
	Status status,
	Long upcomingMeetingCount,
	@Nullable
	GroupLikeStatus likeStatus
)
{

}
