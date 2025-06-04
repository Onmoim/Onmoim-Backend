package com.onmoim.server.meeting.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.meeting.dto.request.MeetingCreateRequest;
import com.onmoim.server.meeting.dto.response.MeetingPageResponse;
import com.onmoim.server.meeting.dto.response.MeetingResponse;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.service.MeetingQueryService;
import com.onmoim.server.meeting.service.MeetingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/groups/{groupId}/meetings")
@RequiredArgsConstructor
@Tag(name = "Meeting API", description = "일정 관리 API")
public class MeetingController {

	private final MeetingService meetingService;
	private final MeetingQueryService meetingQueryService;

	/**
	 * 일정 생성
	 */
	@PostMapping
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
	@GetMapping("/{meetingId}")
	@Operation(summary = "일정 단건 조회", description = "특정 일정의 상세 정보를 조회합니다.")
	public ResponseEntity<MeetingResponse> getMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		Meeting meeting = meetingQueryService.getById(meetingId);
		return ResponseEntity.ok(MeetingResponse.from(meeting));
	}

	/**
	 * 일정 목록 조회 (페이징)
	 */
	@GetMapping
	@Operation(summary = "일정 목록 조회", description = "모임의 일정 목록을 페이징으로 조회합니다. 타입별 필터링이 가능합니다.")
	public ResponseEntity<MeetingPageResponse> getMeetings(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@RequestParam(defaultValue = "0") @Parameter(description = "페이지 번호 (0부터 시작)") int page,
		@RequestParam(defaultValue = "10") @Parameter(description = "페이지 크기") int size,
		@RequestParam(required = false) @Parameter(description = "일정 유형 필터") MeetingType type
	) {
		Pageable pageable = PageRequest.of(page, size);
		Page<Meeting> meetingPage;

		if (type != null) {
			meetingPage = meetingQueryService.findByGroupIdAndType(groupId, type, pageable);
		} else {
			meetingPage = meetingQueryService.findByGroupId(groupId, pageable);
		}

		return ResponseEntity.ok(MeetingPageResponse.from(meetingPage));
	}

	/**
	 * 일정 참석 신청
	 */
	@PostMapping("/{meetingId}/join")
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
	@PostMapping("/{meetingId}/leave")
	@Operation(summary = "일정 참석 취소", description = "일정 참석을 취소합니다.")
	public ResponseEntity<Void> leaveMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		meetingService.leaveMeeting(meetingId);
		return ResponseEntity.ok().build();
	}
}
