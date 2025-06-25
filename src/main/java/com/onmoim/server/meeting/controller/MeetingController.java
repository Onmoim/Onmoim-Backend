package com.onmoim.server.meeting.controller;

import com.onmoim.server.meeting.dto.response.CursorPageResponseDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.meeting.dto.request.MeetingCreateRequestDto;
import com.onmoim.server.meeting.dto.request.MeetingUpdateRequestDto;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.service.MeetingQueryService;
import com.onmoim.server.meeting.service.MeetingService;
import com.onmoim.server.meeting.service.MeetingFacadeService;
import com.onmoim.server.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Meeting API", description = "일정 관리 API")
public class MeetingController {

	private final MeetingService meetingService;
	private final MeetingQueryService meetingQueryService;
	private final MeetingFacadeService meetingFacadeService;

	/**
	 * 내가 속한 모든 모임의 예정된 일정 조회
	 */
	@GetMapping("/meetings/my")
	@Operation(
		summary = "내 예정 일정 조회 (커서 페이징)",
		description = "내가 속한 모든 모임의 예정된 일정을 시간순으로 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CursorPageResponseDto.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<ResponseHandler<CursorPageResponseDto<MeetingResponseDto>>> getMyUpcomingMeetings(
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 ID (첫 페이지는 생략)") Long cursorId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size
	) {
		Long userId = getCurrentUserId();
		CursorPageResponseDto<MeetingResponseDto> response =
			meetingQueryService.getMyUpcomingMeetings(userId, cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	/**
	 * 일정 생성 (이미지 포함)
	 */
	@PostMapping(value = "/groups/{groupId}/meetings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
		summary = "일정 생성",
		description = "새로운 일정을 생성합니다. 정기모임은 모임장만, 번개모임은 모임원이 생성 가능합니다. 대표 이미지도 함께 업로드할 수 있습니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "생성 성공(생성된 일정 ID 반환)",
			content = @Content(
				schema = @Schema(implementation = Long.class))),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "403", description = "권한 없음"),
		@ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<Long>> createMeeting(
		@PathVariable
		@Parameter(description = "모임 ID") Long groupId,
		@Valid @RequestPart("request") MeetingCreateRequestDto request,
		@RequestPart(value = "image", required = false)
		@Parameter(description = "일정 대표 이미지") MultipartFile image
	) {
		Long meetingId = meetingService.createMeeting(groupId, request, image);
		return ResponseEntity.ok(ResponseHandler.response(meetingId));
	}

	/**
	 * 일정 단건 조회
	 */
	@GetMapping("/groups/{groupId}/meetings/{meetingId}")
	@Operation(summary = "일정 단건 조회", description = "특정 일정의 상세 정보를 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = MeetingResponseDto.class))),
		@ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<MeetingResponseDto>> getMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		Meeting meeting = meetingQueryService.getById(meetingId);
		return ResponseEntity.ok(ResponseHandler.response(MeetingResponseDto.from(meeting)));
	}

	/**
	 * 그룹별 예정된 일정 목록 조회
	 */
	@GetMapping("/groups/{groupId}/meetings")
	@Operation(
		summary = "그룹별 예정된 일정 목록 조회 (커서 페이징)",
		description = "특정 모임의 예정된 일정 목록을 시간순으로 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CursorPageResponseDto.class))),
		@ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<CursorPageResponseDto<MeetingResponseDto>>> getGroupUpcomingMeetings(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 ID (첫 페이지는 생략)") Long cursorId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size,
		@RequestParam(required = false)
		@Parameter(description = "일정 유형 필터 (REGULAR: 정기모임, FLASH: 번개모임)") MeetingType type
	) {
		CursorPageResponseDto<MeetingResponseDto> response =
			meetingQueryService.getUpcomingMeetingsInGroup(groupId, type, cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	/**
	 * 일정 참석 신청
	 */
	@PostMapping("/groups/{groupId}/meetings/{meetingId}/join")
	@Operation(summary = "일정 참석 신청", description = "일정에 참석 신청을 합니다. 인원 제한이 있으며 선착순으로 제한됩니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "신청 성공"),
		@ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "정원 초과")
	})
	public ResponseEntity<ResponseHandler<Void>> joinMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		meetingFacadeService.joinMeeting(meetingId);
		return ResponseEntity.ok(ResponseHandler.response(null));
	}

	/**
	 * 일정 참석 취소
	 */
	@PostMapping("/groups/{groupId}/meetings/{meetingId}/leave")
	@Operation(summary = "일정 참석 취소", description = "일정 참석을 취소합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "취소 성공"),
		@ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<Void>> leaveMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		meetingFacadeService.leaveMeeting(meetingId);
		return ResponseEntity.ok(ResponseHandler.response(null));
	}

	/**
	 * 일정 수정 (이미지 포함)
	 */
	@PutMapping(value = "/groups/{groupId}/meetings/{meetingId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
		summary = "일정 수정",
		description = "일정 정보를 수정합니다. 모임장만 수정 가능하니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "403", description = "권한 없음"),
		@ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<Void>> updateMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId,
		@Valid @RequestPart("request") MeetingUpdateRequestDto request,
		@RequestPart(value = "image", required = false)
		@Parameter(description = "새 대표 이미지 (선택적)") MultipartFile image
	) {

		meetingFacadeService.updateMeeting(meetingId, request);

		if (image != null && !image.isEmpty()) {
			meetingService.updateMeetingImage(meetingId, image);
		}

		return ResponseEntity.ok(ResponseHandler.response(null));
	}

	/**
	 * 일정 삭제
	 */
	@DeleteMapping("/groups/{groupId}/meetings/{meetingId}")
	@Operation(
		summary = "일정 삭제",
		description = "일정을 삭제합니다. 모임장만 삭제 가능합니다. 관련된 모든 데이터(참석자, 이미지)가 함께 삭제됩니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "삭제 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "403", description = "권한 없음 (모임장만 삭제 가능)"),
		@ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<Void>> deleteMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		meetingService.deleteMeeting(meetingId);
		return ResponseEntity.ok(ResponseHandler.response(null));
	}

	/**
	 * 현재 사용자 ID 조회
	 */
	private Long getCurrentUserId() {
		CustomUserDetails principal =
			(CustomUserDetails) SecurityContextHolder.getContextHolderStrategy()
				.getContext()
				.getAuthentication()
				.getPrincipal();
		return principal.getUserId();
	}
}
