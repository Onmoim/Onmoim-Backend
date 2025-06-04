package com.onmoim.server.meeting.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.dto.CursorPageResponse;
import com.onmoim.server.meeting.dto.request.MeetingCreateRequest;
import com.onmoim.server.meeting.dto.response.MeetingResponse;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.service.MeetingQueryService;
import com.onmoim.server.meeting.service.MeetingService;
import com.onmoim.server.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Meeting API", description = "일정 관리 API")
public class MeetingController {

	private final MeetingService meetingService;
	private final MeetingQueryService meetingQueryService;

	/**
	 * 내가 속한 모든 모임의 예정된 일정 조회 (커서 기반)
	 */
	@GetMapping("/meetings/my")
	@Operation(summary = "내 예정 일정 조회", description = "내가 속한 모든 모임의 예정된 일정을 시간순으로 조회합니다. 무한 스크롤을 위한 커서 기반 페이징입니다.")
	public ResponseEntity<CursorPageResponse<MeetingResponse>> getMyUpcomingMeetings(
		@RequestParam(required = false) @Parameter(description = "커서 ID (다음 페이지 조회용)") Long cursorId,
		@RequestParam(defaultValue = "10") @Parameter(description = "페이지 크기") int size
	) {
		Long userId = getCurrentUserId();
		CursorPageResponse<Meeting> meetingPage = meetingQueryService.getUpcomingMeetingsByUserId(userId, cursorId, size);

		CursorPageResponse<MeetingResponse> response = CursorPageResponse.<MeetingResponse>builder()
			.content(meetingPage.getContent().stream().map(MeetingResponse::from).toList())
			.hasNext(meetingPage.isHasNext())
			.nextCursorId(meetingPage.getNextCursorId())
			.build();

		return ResponseEntity.ok(response);
	}

	/**
	 * 일정 생성
	 */
	@PostMapping("/groups/{groupId}/meetings")
	@Operation(summary = "일정 생성", description = "새로운 일정을 생성합니다. 정기모임은 모임장만, 번개모임은 모임원이 생성 가능합니다.")
	public ResponseEntity<Long> createMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@Valid @RequestBody MeetingCreateRequest request
	) {
		Long meetingId = meetingService.createMeeting(groupId, request);
		return ResponseEntity.ok(meetingId);
	}

	/**
	 * 일정 단건 조회
	 */
	@GetMapping("/groups/{groupId}/meetings/{meetingId}")
	@Operation(summary = "일정 단건 조회", description = "특정 일정의 상세 정보를 조회합니다.")
	public ResponseEntity<MeetingResponse> getMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		Meeting meeting = meetingQueryService.getById(meetingId);
		return ResponseEntity.ok(MeetingResponse.from(meeting));
	}

	/**
	 * 그룹별 예정된 일정 목록 조회 (커서 기반)
	 */
	@GetMapping("/groups/{groupId}/meetings")
	@Operation(summary = "그룹별 예정된 일정 목록 조회", description = "특정 모임의 예정된 일정 목록을 시간순으로 조회합니다. 과거 일정은 제외됩니다. 무한 스크롤을 위한 커서 기반 페이징입니다.")
	public ResponseEntity<CursorPageResponse<MeetingResponse>> getMeetingsCursor(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@RequestParam(required = false) @Parameter(description = "커서 ID (다음 페이지 조회용)") Long cursorId,
		@RequestParam(defaultValue = "10") @Parameter(description = "페이지 크기") int size,
		@RequestParam(required = false) @Parameter(description = "일정 유형 필터 (REGULAR: 정기모임, FLASH: 번개모임)") MeetingType type
	) {
		CursorPageResponse<Meeting> meetingPage = meetingQueryService.getMeetingsByGroupIdAndType(groupId, type, cursorId, size);

		CursorPageResponse<MeetingResponse> response = CursorPageResponse.<MeetingResponse>builder()
			.content(meetingPage.getContent()
				.stream()
				.map(MeetingResponse::from)
				.toList())
			.hasNext(meetingPage.isHasNext())
			.nextCursorId(meetingPage.getNextCursorId())
			.build();

		return ResponseEntity.ok(response);
	}

	/**
	 * 일정 참석 신청
	 */
	@PostMapping("/groups/{groupId}/meetings/{meetingId}/join")
	@Operation(summary = "일정 참석 신청", description = "일정에 참석 신청을 합니다.인원 제한이 있으며 선착순으로 제한됩니다. ")
	public ResponseEntity<Void> joinMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		meetingService.joinMeeting(meetingId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 일정 참석 취소
	 */
	@PostMapping("/groups/{groupId}/meetings/{meetingId}/leave")
	@Operation(summary = "일정 참석 취소", description = "일정 참석을 취소합니다.")
	public ResponseEntity<Void> leaveMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		meetingService.leaveMeeting(meetingId);
		return ResponseEntity.ok().build();
	}

	/**
	 * 현재 사용자 ID 조회
	 */
	private Long getCurrentUserId() {
		CustomUserDetails principal = (CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
}
