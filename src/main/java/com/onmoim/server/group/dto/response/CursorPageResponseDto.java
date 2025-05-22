package com.onmoim.server.group.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class CursorPageResponseDto <T> {
	private List<T> content;   // 데이터 목록
	private Long totalCount;   // 전체 크기
	private boolean hasNext;   // 다음 페이지 존재 유무
	private Long cursorId;     // 다음 요청에서 사용할 커서 ID
}
