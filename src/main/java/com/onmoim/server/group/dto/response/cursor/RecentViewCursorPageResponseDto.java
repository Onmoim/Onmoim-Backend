package com.onmoim.server.group.dto.response.cursor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentViewCursorPageResponseDto<T> {

	private List<T> content;
	private boolean hasNext;
	private LocalDateTime nextCursorViewedAt;
	private Long nextCursorLogId;

	public static <T> RecentViewCursorPageResponseDto<T> of(List<T> content, boolean hasNext, LocalDateTime cursorViewedAt, Long cursorLogId) {
		return RecentViewCursorPageResponseDto.<T>builder()
			.content(content)
			.hasNext(hasNext)
			.nextCursorViewedAt(cursorViewedAt)
			.nextCursorLogId(cursorLogId)
			.build();
	}

	public static <T> RecentViewCursorPageResponseDto<T> empty() {
		return RecentViewCursorPageResponseDto.<T>builder()
			.content(List.of())
			.hasNext(false)
			.nextCursorViewedAt(null)
			.nextCursorLogId(null)
			.build();
	}

}
