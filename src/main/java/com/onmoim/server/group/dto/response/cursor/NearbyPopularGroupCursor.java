package com.onmoim.server.group.dto.response.cursor;

import java.util.List;

import com.onmoim.server.group.dto.response.GroupInfoResponseDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 주변 인기 모임 조회용
 */
public record NearbyPopularGroupCursor(
	@Schema(description = "다음 페이지 존재 유무")
	boolean hasNext,
	@Schema(description = "[커서] ID, 마지막 모임 ID")
	Long cursorId,
	@Schema(description = "[커서] 회원 수, 마지막 회원 수")
	Long memberCount
)
{
	public static NearbyPopularGroupCursor of(
		boolean hasNext,
		List<GroupInfoResponseDto> response
	)
	{
		GroupInfoResponseDto responseDto = extractInfo(response);
		return new NearbyPopularGroupCursor(
			hasNext,
			responseDto.groupId(),
			responseDto.memberCount()
		);
	}

	private static GroupInfoResponseDto extractInfo(List<GroupInfoResponseDto> list) {
		return list.getLast();
	}
}
