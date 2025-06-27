package com.onmoim.server.group.dto;

public record ActiveGroupDetail (
	Long groupId,
	String imgUrl,
	String name,
	String dong,
	String category,
	Long memberCount
)
{

}
