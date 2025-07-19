package com.onmoim.server.group.dto;

/**
 * @param groupId - 모임 id
 * @param imageUrl - 모임 이미지
 * @param name - 모임 이름
 * @param memberCount - 모임 인원
 * @param dong - 모임 지역
 * @param category - 모임 카테고리
 */
public record PopularGroupSummary (
	Long groupId,
	String imageUrl,
	String name,
	Long memberCount,
	String dong,
	String category
)
{

}
