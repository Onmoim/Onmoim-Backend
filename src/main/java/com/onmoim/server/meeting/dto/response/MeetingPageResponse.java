package com.onmoim.server.meeting.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import com.onmoim.server.meeting.entity.Meeting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "일정 목록 페이징 응답")
public class MeetingPageResponse {

	@Schema(description = "일정 목록")
	private List<MeetingResponse> meetings;

	@Schema(description = "현재 페이지 번호", example = "0")
	private int page;

	@Schema(description = "페이지 크기", example = "10")
	private int size;

	@Schema(description = "전체 요소 수", example = "100")
	private long totalElements;

	@Schema(description = "전체 페이지 수", example = "10")
	private int totalPages;

	@Schema(description = "첫 번째 페이지 여부", example = "true")
	private boolean first;

	@Schema(description = "마지막 페이지 여부", example = "false")
	private boolean last;

	public static MeetingPageResponse from(Page<Meeting> meetingPage) {
		List<MeetingResponse> meetings = meetingPage.getContent()
			.stream()
			.map(MeetingResponse::from)
			.toList();

		return MeetingPageResponse.builder()
			.meetings(meetings)
			.page(meetingPage.getNumber())
			.size(meetingPage.getSize())
			.totalElements(meetingPage.getTotalElements())
			.totalPages(meetingPage.getTotalPages())
			.first(meetingPage.isFirst())
			.last(meetingPage.isLast())
			.build();
	}
} 