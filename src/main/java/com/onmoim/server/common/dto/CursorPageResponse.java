package com.onmoim.server.common.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커서 기반 페이징 응답")
public class CursorPageResponse<T> {

	@Schema(description = "데이터 목록")
	private List<T> content;

	@Schema(description = "다음 페이지 존재 여부", example = "true")
	private boolean hasNext;

	@Schema(description = "다음 페이지 요청 시 사용할 커서 ID", example = "123")
	private Long nextCursorId;

	/**
	 * 커서 기반 응답 생성
	 */
	public static <T> CursorPageResponse<T> of(List<T> content, boolean hasNext, Long nextCursorId) {
		return CursorPageResponse.<T>builder()
			.content(content)
			.hasNext(hasNext)
			.nextCursorId(nextCursorId)
			.build();
	}
} 