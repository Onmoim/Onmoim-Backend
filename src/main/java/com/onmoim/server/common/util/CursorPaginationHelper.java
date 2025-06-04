package com.onmoim.server.common.util;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Slice;

import com.onmoim.server.common.dto.CursorPageResponse;

import lombok.experimental.UtilityClass;

/**
 * 커서 기반 페이징 공통 유틸리티
 */
@UtilityClass
public class CursorPaginationHelper {

	/**
	 * Slice를 CursorPageResponse로 변환
	 *
	 * @param <T> 엔티티 타입
	 * @param slice            Spring Data Slice
	 * @param cursorExtractor  다음 커서 ID 추출 함수
	 * @return 커서 기반 페이징 응답
	 */
	public static <T> CursorPageResponse<T> fromSlice(
		Slice<T> slice,
		Function<T, Long> cursorExtractor
	) {
		List<T> content = slice.getContent();
		boolean hasNext = slice.hasNext();

		// 다음 커서 ID 계산
		Long nextCursorId = content.isEmpty()
			? null
			: cursorExtractor.apply(content.getLast());

		return CursorPageResponse.of(content, hasNext, nextCursorId);
	}

	/**
	 * ID 기반 Slice 변환
	 */
	public static <T> CursorPageResponse<T> fromSliceWithId(
		Slice<T> slice,
		Function<T, Long> idExtractor
	) {
		return fromSlice(slice, idExtractor);
	}
}
