package com.onmoim.server.group.dto.response.cursor;

import java.util.List;

import com.onmoim.server.group.dto.response.GroupMembersResponseDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 모임 회원 리스트 조회용
 */
public record MemberListCursor(
	@Schema(description = "다음 페이지 존재 유무")
	boolean hasNext,
	@Schema(description = "[커서] ID, 마지막 회원 ID")
	Long lastMemberId,
	@Schema(description = "전체 회원 수")
	Long groupMemberCount
)
{
	public static MemberListCursor of(
		boolean hasNext,
		List<GroupMembersResponseDto> list,
		Long groupMemberCount
	)
	{
		return new MemberListCursor(
			hasNext,
			extractCursorId(list),
			groupMemberCount);
	}

	private static Long extractCursorId(List<GroupMembersResponseDto> list) {
		return list.getLast().memberId();
	}
}
