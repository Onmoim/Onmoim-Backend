package com.onmoim.server.group.dto.response;

import java.util.List;

import com.onmoim.server.group.entity.GroupUser;

import io.swagger.v3.oas.annotations.media.Schema;

public record CursorPageResponseDto <T> (
	@Schema(description = "데이터 목록")
	List<T> content,
	@Schema(description = "전체 크기")
	Long totalCount,
	@Schema(description = "다음 페이지 존재 유무")
	boolean hasNext,
	@Schema(description = "다음 요청에서 사용할 커서 ID: 응답 받은 커서 ID 그대로 보내주세요")
	Long cursorId
) {
	public static CursorPageResponseDto<GroupMembersResponseDto> of(
		List<GroupUser> groupMembers,
		int size,
		Long totalCount
	){
		boolean hasNext = hasNext(size, groupMembers.size());
		extractContent(groupMembers, hasNext);

		List<GroupMembersResponseDto> content = groupMembers.stream()
			.map(GroupMembersResponseDto::of).toList();

		Long cursorId = content.isEmpty() ? null : content.getLast().userId();

		return new CursorPageResponseDto<>(
			content,
			totalCount,
			hasNext,
			cursorId
		);
	}

	private static boolean hasNext(int requestSize, int resultSize) {
		return resultSize > requestSize;
	}

	private static void extractContent(List<?> result, boolean hasNext) {
		if(hasNext) {
			result.removeLast();
		}
	}
}
