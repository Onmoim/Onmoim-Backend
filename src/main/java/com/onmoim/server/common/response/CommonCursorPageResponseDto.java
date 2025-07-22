package com.onmoim.server.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonCursorPageResponseDto<T> {

	private List<T> content;     // 데이터 목록
	private boolean hasNext;     // 다음 페이지 존재 여부
	private Long nextCursorId;   // 다음 페이지 요청 시 사용할 커서 ID (ID 기반)

	public static <T> CommonCursorPageResponseDto<T> of(List<T> content, boolean hasNext, Long nextCursorId) {
		return CommonCursorPageResponseDto.<T>builder()
			.content(content)
			.hasNext(hasNext)
			.nextCursorId(nextCursorId)
			.build();
	}

	public static <T> CommonCursorPageResponseDto<T> empty() {
		return CommonCursorPageResponseDto.<T>builder()
			.content(List.of())
			.hasNext(false)
			.nextCursorId(null)
			.build();
	}
}
