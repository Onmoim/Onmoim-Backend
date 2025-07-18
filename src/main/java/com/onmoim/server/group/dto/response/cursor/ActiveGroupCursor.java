package com.onmoim.server.group.dto.response.cursor;

import java.util.List;

import com.onmoim.server.group.dto.response.GroupInfoResponseDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ActiveGroupCursor (
	@Schema(description = "다음 페이지 존재 유무")
	boolean hasNext,
	@Schema(description = "[커서] ID, 마지막 모임 ID")
	Long lastGroupId,
	@Schema(description = "[커서] 다가오는 일정 수, 마지막 일정 수")
	Long meetingCount
)
{
	public static ActiveGroupCursor of(
		boolean hasNext,
		List<GroupInfoResponseDto> response
	)
	{
		if(hasNext){
			GroupInfoResponseDto responseDto = extractInfo(response);
			return new ActiveGroupCursor(
				hasNext,
				responseDto.groupId(),
				responseDto.upcomingMeetingCount()
			);
		}
		return new ActiveGroupCursor(
			hasNext,
			null,
			null
		);
	}

	private static GroupInfoResponseDto extractInfo(List<GroupInfoResponseDto> list) {
		return list.getLast();
	}
}

