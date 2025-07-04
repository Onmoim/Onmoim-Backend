package com.onmoim.server.group.dto.response.cursor;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CursorUtilClass {

	// 다음 페이지 유무
	public static <T> boolean hasNext(List<T> content, int requestSize) {
		return content.size() > requestSize;
	}

	// 요청 크기에 맞게 자르기
	public static <T> List<T> extractContent(List<T> content, boolean hasNext, int requestSize) {
		return hasNext ? content.subList(0, requestSize) : content;
	}
}
