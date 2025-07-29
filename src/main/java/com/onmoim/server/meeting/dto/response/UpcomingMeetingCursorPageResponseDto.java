package com.onmoim.server.meeting.dto.response;

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
public class UpcomingMeetingCursorPageResponseDto<T> {

	private List<T> content;
	private boolean hasNext;
	private LocalDateTime nextCursorStartAt;
	private Long nextCursorId;

	public static <T> UpcomingMeetingCursorPageResponseDto<T> of(List<T> content, boolean hasNext, LocalDateTime nextCursorStartAt, Long nextCursorId) {
		return UpcomingMeetingCursorPageResponseDto.<T>builder()
			.content(content)
			.hasNext(hasNext)
			.nextCursorStartAt(nextCursorStartAt)
			.nextCursorId(nextCursorId)
			.build();
	}

	public static <T> UpcomingMeetingCursorPageResponseDto<T> empty() {
		return UpcomingMeetingCursorPageResponseDto.<T>builder()
			.content(List.of())
			.hasNext(false)
			.nextCursorStartAt(null)
			.nextCursorId(null)
			.build();
	}

}
