package com.onmoim.server.group.dto;

/**
 * 내 주변 인기 모임, 활동이 활발한 모임 조회 조건
 */
public record ReadCondition (
	Long cursorId,
	int size
)
{
	public static ReadCondition of (Long cursorId, int size) {
		return new ReadCondition(cursorId, size);
	}
}
