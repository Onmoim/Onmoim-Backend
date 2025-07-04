package com.onmoim.server.group.dto;

public record ActiveGroupRelation (
	Long groupId,
	Long userId,
	String status
)
{

}
