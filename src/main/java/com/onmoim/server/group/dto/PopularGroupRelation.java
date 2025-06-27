package com.onmoim.server.group.dto;

/**
 * @param groupId 모임 id
 * @param status 현재 사용자와 모임 관계
 * @param upcomingMeetingCount 다가오는 일정 개수
 */
public record PopularGroupRelation(
	Long groupId,
	String status,
	Long upcomingMeetingCount
)
{

}
