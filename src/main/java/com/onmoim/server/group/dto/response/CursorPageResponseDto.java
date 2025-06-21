package com.onmoim.server.group.dto.response;

import java.util.List;

import com.onmoim.server.group.dto.GroupMember;

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
		List<GroupMember> groupMembers,
		int size,
		Long totalCount
	){
		boolean hasNext = hasNext(size, groupMembers.size());
		List<GroupMember> pageContent = extractContent(groupMembers, hasNext, size);

		List<GroupMembersResponseDto> content = pageContent.stream()
			.map(GroupMembersResponseDto::of).toList();

		Long cursorId = content.isEmpty() ? null : content.getLast().memberId();

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

	private static <T> List<T> extractContent (List<T> result, boolean hasNext, int size) {
		return hasNext ? result.subList(0, size) : result;
	}
}
