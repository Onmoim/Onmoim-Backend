package com.onmoim.server.meeting.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.dto.CursorPageResponse;
import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.common.s3.dto.FileUploadResponseDto;
import com.onmoim.server.meeting.dto.request.MeetingCreateRequestDto;
import com.onmoim.server.meeting.dto.request.MeetingUpdateRequestDto;
import com.onmoim.server.meeting.dto.response.MeetingResponseDto;
import com.onmoim.server.meeting.entity.Meeting;
import com.onmoim.server.meeting.entity.MeetingType;
import com.onmoim.server.meeting.service.MeetingQueryService;
import com.onmoim.server.meeting.service.MeetingService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Meeting API", description = "일정 관리 API")
public class MeetingController {

	private final MeetingService meetingService;
	private final MeetingQueryService meetingQueryService;

	/**
	 * 내가 속한 모든 모임의 예정된 일정 조회
	 */
	@GetMapping("/meetings/my")
	@Operation(
		summary = "내 예정 일정 조회",
		description = "내가 속한 모든 모임의 예정된 일정을 시간순으로 조회합니다. 무한 스크롤을 위한 페이지네이션입니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CursorPageResponse.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<ResponseHandler<CursorPageResponse<MeetingResponseDto>>> getMyUpcomingMeetings(
		@RequestParam(required = false)
		@Parameter(description = "마지막 모임 ID (다음 페이지 조회용)") Long lastId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size
	) {
		Long userId = getCurrentUserId();
		CursorPageResponse<MeetingResponseDto> response =
			meetingQueryService.getMyUpcomingMeetings(userId, lastId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	/**
	 * 일정 생성
	 */
	@PostMapping("/groups/{groupId}/meetings")
	@Operation(
		summary = "일정 생성",
		description = "새로운 일정을 생성합니다. 정기모임은 모임장만, 번개모임은 모임원이 생성 가능합니다.")
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
		@Valid @RequestBody MeetingCreateRequestDto request
	) {
		Long meetingId = meetingService.createMeeting(groupId, request);
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
		summary = "그룹별 예정된 일정 목록 조회",
		description = "특정 모임의 예정된 일정 목록을 시간순으로 조회합니다. 과거 일정은 제외됩니다. 무한 스크롤을 위한 페이지네이션입니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CursorPageResponse.class))),
		@ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<CursorPageResponse<MeetingResponseDto>>> getGroupUpcomingMeetings(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@RequestParam(required = false)
		@Parameter(description = "마지막 모임 ID (다음 페이지 조회용)") Long lastId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size,
		@RequestParam(required = false)
		@Parameter(description = "일정 유형 필터 (REGULAR: 정기모임, FLASH: 번개모임)") MeetingType type
	) {
		CursorPageResponse<MeetingResponseDto> response =
			meetingQueryService.getUpcomingMeetingsInGroup(groupId, type, lastId, size);
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
		meetingService.joinMeeting(meetingId);
		return ResponseEntity.ok(ResponseHandler.response(null));
	}

	/**
	 * 일정 참석 취소
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
		meetingService.leaveMeeting(meetingId);
		return ResponseEntity.ok(ResponseHandler.response(null));
	}

	/**
	 * 일정 수정
	 */
	@PutMapping("/groups/{groupId}/meetings/{meetingId}")
	@Operation(summary = "일정 수정", description = "일정 정보를 수정합니다. 모임장만 수정 가능하며, 시작 24시간 전까지만 수정할 수 있습니다.")
	public ResponseEntity<ResponseHandler<Void>> updateMeeting(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId,
		@Valid @RequestBody MeetingUpdateRequestDto request
	) {
		meetingService.updateMeeting(meetingId, request);
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
	 * 일정 이미지 업로드
	 */
	@PostMapping(value = "/groups/{groupId}/meetings/{meetingId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
		summary = "일정 이미지 업로드",
		description = "일정의 대표 이미지를 업로드합니다. 정기모임은 모임장만, 번개모임은 모임장 또는 주최자가 업로드 가능합니다."
	)
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "이미지 업로드 성공",
			content = @Content(schema = @Schema(implementation = FileUploadResponseDto.class))
		),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "403", description = "권한 없음"),
		@ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<FileUploadResponseDto>> uploadMeetingImage(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId,
		@RequestParam("file") @Parameter(description = "업로드할 이미지 파일") MultipartFile file
	) {
		FileUploadResponseDto response = meetingService.updateMeetingImage(meetingId, file);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	/**
	 * 일정 이미지 삭제
	 */
	@DeleteMapping("/groups/{groupId}/meetings/{meetingId}/image")
	@Operation(
		summary = "일정 이미지 삭제",
		description = "일정의 대표 이미지를 삭제합니다. 모든 일정 타입에서 모임장만 삭제 가능합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "이미지 삭제 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청"),
		@ApiResponse(responseCode = "403", description = "권한 없음 (모임장만 가능)"),
		@ApiResponse(responseCode = "404", description = "일정을 찾을 수 없음")
	})
	public ResponseEntity<ResponseHandler<Void>> deleteMeetingImage(
		@PathVariable @Parameter(description = "모임 ID") Long groupId,
		@PathVariable @Parameter(description = "일정 ID") Long meetingId
	) {
		meetingService.updateMeetingImage(meetingId, null);
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
