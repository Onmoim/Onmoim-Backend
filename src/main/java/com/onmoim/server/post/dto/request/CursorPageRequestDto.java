package com.onmoim.server.post.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageRequestDto {
    private Long cursorId; // 마지막으로 조회한 게시글 ID
    private int size;      // 조회할 페이지 크기

	public int getSize() {
		if (size <= 0) {
			return 10;
		}
		return size;
	}
}
