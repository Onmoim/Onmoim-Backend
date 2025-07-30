package com.onmoim.server.group.controller;

import static org.springframework.http.HttpStatus.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.onmoim.server.group.dto.response.*;
import com.onmoim.server.group.dto.response.cursor.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.chat.domain.dto.ChatRoomResponse;
import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.group.dto.ActiveGroup;
import com.onmoim.server.group.dto.ActiveGroupDetail;
import com.onmoim.server.group.dto.ActiveGroupRelation;
import com.onmoim.server.group.dto.PopularGroupRelation;
import com.onmoim.server.group.dto.PopularGroupSummary;
import com.onmoim.server.group.dto.GroupDetail;
import com.onmoim.server.group.dto.GroupMember;
import com.onmoim.server.group.dto.request.GroupCreateRequestDto;
import com.onmoim.server.group.dto.request.GroupUpdateRequestDto;
import com.onmoim.server.group.dto.request.MemberIdRequestDto;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.service.GroupService;
import com.onmoim.server.meeting.dto.MeetingDetail;
import com.onmoim.server.meeting.service.MeetingService;
import com.onmoim.server.common.response.CommonCursorPageResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Group", description = "모임 관련 API")
public class GroupController {
	private final GroupService groupService;
	private final MeetingService meetingService;

	@Operation(
		summary = "모임 생성",
		description = "모임을 생성합니다. 모임 생성 성공 시 생성된 모임 ID가 반환됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "201",
			description = "모임 생성 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "Request DTO 검증 실패 (입력값 형식 오류, 필수값 누락 등)"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 카테고리 ID 또는 지역 ID"),
		@ApiResponse(
			responseCode = "409",
			description = "이미 존재하는 모임 이름")
	})
	@PostMapping("/v1/groups")
	public ResponseEntity<ResponseHandler<ChatRoomResponse>> createGroup(
		@RequestBody @Valid GroupCreateRequestDto request
	)
	{
		ChatRoomResponse response = groupService.createGroup(
			request.categoryId(),
			request.locationId(),
			request.name(),
			request.description(),
			request.capacity()
		);
		return ResponseEntity.status(CREATED).body(ResponseHandler.response(response));
	}

	@Operation(
		summary = "모임 수정",
		description = "모임을 수정합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 수정 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "Request DTO 검증 실패 (입력값 형식 오류, 필수값 누락 등)"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "403",
			description = "권한이 부족합니다. 모임장만 수정 가능"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임"),
		@ApiResponse(
			responseCode = "409",
			description = "정원 제한 위반")
	})
	@PatchMapping("/v1/groups/{groupId}")
	public ResponseEntity<ResponseHandler<String>> updateGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId,
		@Parameter(description = "모임 수정 정보")
		@Valid @RequestPart(value = "request") GroupUpdateRequestDto request,
		@Parameter(description = "이미지 파일")
		@RequestPart(value = "file", required = false) MultipartFile file
	)
	{
		groupService.updateGroup(groupId, request.description(), request.capacity(), file);
		return ResponseEntity.ok(ResponseHandler.response("수정 성공"));
	}

	@Operation(
		summary = "모임 가입",
		description = "모임을 가입합니다. 모임 가입 성공 시 '채팅방 구독 Destination' 문자열이 반환됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "201",
			description = "/topic/chat.room.1"),
		@ApiResponse(
			responseCode = "400",
			description = "이미 가입된 회원이거나, 벤 처리된 회원"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임"),
		@ApiResponse(
			responseCode = "409",
			description = "정원이 이미 꽉 찬 모임"),
		@ApiResponse(
			responseCode = "429",
			description = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
	})
	@PostMapping("/v1/groups/{groupId}/join")
	public ResponseEntity<ResponseHandler<Map<String,String>>> joinGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId
	)
	{
		String subscribeDestination = groupService.joinGroup(groupId);
		return ResponseEntity
			.status(CREATED)
			.body(ResponseHandler.response(Map.of("subscribeDestination", subscribeDestination)));
	}

	@Operation(
		summary = "모임 좋아요(찜)",
		description = "현재 사용자가 좋아요 또는 좋아요 취소를 합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "업데이트 이후 상태를 반환합니다 좋아요 상태: LIKE, 좋아요 취소 상태: PENDING"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임"),
		@ApiResponse(
			responseCode = "429",
			description = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
	})
	@PostMapping("/v1/groups/{groupId}/like")
	public ResponseEntity<ResponseHandler<String>> likeGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId
	)
	{
		GroupLikeStatus status = groupService.likeGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response(status.name()));
	}

	@Operation(
		summary = "모임 회원 조회",
		description = "모임에 가입한 회원들을 커서 기반 페이징으로 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 회원 조회 성공"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "403",
			description = "권한이 부족합니다. 모임원만 조회 가능"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임")
	})
	@GetMapping("/v1/groups/{groupId}/members")
	public ResponseEntity<ResponseHandler<CursorPageResponseDto<GroupMembersResponseDto, MemberListCursor>>> getGroupMembers(
		@Parameter(description = "모임 ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId,
		@Parameter(description = "마지막 회원 ID(커서 용도)", in = ParameterIn.QUERY)
		@RequestParam(required = false) Long lastMemberId,
		@Parameter(description = "페이지 크기 (고정 크기 = 10)", in = ParameterIn.QUERY)
		@RequestParam(required = false, defaultValue = "10") int requestSize
	)
	{
		// 모임 회원 조회
		List<GroupMember> groupMembers = groupService.readGroupMembers(groupId, lastMemberId, requestSize);

		// 모임 전체 회원 수 조회
		Long totalMemberCount = groupService.groupMemberCount(groupId);

		// 다음 페이지 유무
		boolean hasNext = CursorUtilClass.hasNext(groupMembers, requestSize);

		// 요청 크기에 맞게 가공
		List<GroupMember> extractResult = CursorUtilClass.extractContent(groupMembers, hasNext, requestSize);

		// 응답 DTO 변환
		List<GroupMembersResponseDto> response = extractResult.stream()
			.map(GroupMembersResponseDto::of).toList();

		// 응답 커서 추출
		MemberListCursor cursorInfo = MemberListCursor.of(hasNext, response, totalMemberCount);

		return ResponseEntity.ok(ResponseHandler.response(CursorPageResponseDto.of(
			response,
			cursorInfo
		)));
	}

	@Operation(
		summary = "모임 삭제",
		description = "모임을 삭제합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 삭제 성공"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "403",
			description = "권한이 부족합니다. 모임장만 삭제 가능"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임"),
	})
	@DeleteMapping("/v1/groups/{groupId}")
	public ResponseEntity<ResponseHandler<String>> deleteGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId
	)
	{
		groupService.deleteGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response("모임 삭제 성공"));
	}

	@Operation(
		summary = "모임 탈퇴",
		description = "현재 로그인한 사용자가 해당 모임에서 탈퇴합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 탈퇴 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "모임을 가입하지 않은 사람의 요청"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임"),
		@ApiResponse(
			responseCode = "409",
			description = "모임장은 권한을 위임하지 않으면 탈퇴할 수 없습니다.")
	})
	@DeleteMapping("/v1/groups/{groupId}/member")
	public ResponseEntity<ResponseHandler<String>> leaveGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId
	)
	{
		groupService.leaveGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response("모임 탍퇴 성공"));
	}

	@Operation(
		summary = "모임장 변경",
		description = "현재 로그인한 사용자가 모임장을 변경합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임장 변경 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "Request DTO 검증 실패 (입력값 형식 오류, 필수값 누락 등)"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근 또는 모임장 변경 대상자가 존재하지 않는 경우"),
		@ApiResponse(
			responseCode = "403",
			description = "현재 사용자가 모임장이 아닌 경우"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임 또는 모임장 변경 대상자가 모임에 존재하지 않습니다"),
		@ApiResponse(
			responseCode = "429",
			description = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
	})
	@PatchMapping("/v1/groups/{groupId}/owner")
	public ResponseEntity<ResponseHandler<String>> transferOwnership(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId,
		@Valid @RequestBody MemberIdRequestDto request
	)
	{
		groupService.transferOwnership(groupId, request.memberId());
		return ResponseEntity.ok(ResponseHandler.response("모임장 변경 성공"));
	}

	@Operation(
		summary = "모임원 강퇴",
		description = "현재 로그인한 사용자가 모임장이면 모임원을 강퇴합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임원 강퇴 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "Request DTO 검증 실패 (입력값 형식 오류, 필수값 누락 등)"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근 또는 모임장 변경 대상자가 존재하지 않는 경우"),
		@ApiResponse(
			responseCode = "403",
			description = "현재 사용자가 모임장이 아닌 경우"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임 또는 모임 강퇴 대상자가 모임에 존재하지 않습니다"),
		@ApiResponse(
			responseCode = "429",
			description = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
	})
	@PostMapping("/v1/groups/{groupId}/ban")
	public ResponseEntity<ResponseHandler<String>> banMember(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId,
		@Valid @RequestBody MemberIdRequestDto request
	)
	{
		groupService.banMember(groupId, request.memberId());
		return ResponseEntity.ok(ResponseHandler.response("모임원 강퇴 성공"));
	}

	@Operation(
		summary = "모임 상세 조회",
		description = "모임 상세한 내용과 d-day가 임박한 상위 2개 일정 조회"
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 상세 조회 성공"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임")
	})
	@GetMapping("/v1/groups/{groupId}")
	public ResponseEntity<ResponseHandler<GroupDetailResponseDto>> getGroupDetail(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId
	)
	{
		// 최근 본 모임 로그 쌓기
		groupService.createGroupViewLog(groupId);

		// 모임 상세 조회
		GroupDetail detail = groupService.readGroup(groupId);
		// 모임원 수 조회
		Long count = groupService.groupMemberCount(groupId);
		// 다가오는 일정 조회 (상위 2개)
		List<MeetingDetail> meetingDetails = meetingService.getUpcomingMeetings(2, groupId);

		return ResponseEntity.ok(ResponseHandler.response(
			GroupDetailResponseDto.of(
					detail,
					count,
					meetingDetails
			)
		));
	}

	@Operation(
		summary = "인기: 활동이 활발한 모임 조회",
		description = "활동이 활발한 모임 조회"
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "활동이 활발한 모임 조회"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근")
	})
	@GetMapping("/v1/groups/active/popular")
	public ResponseEntity<ResponseHandler<CursorPageResponseDto<GroupInfoResponseDto, ActiveGroupCursor>>> get(
		@Parameter(description = "마지막 모임 ID (커서 용도)", in = ParameterIn.QUERY)
		@RequestParam(required = false) Long lastGroupId,
		@Parameter(description = "페이지 크기 (고정 크기 = 10)", in = ParameterIn.QUERY)
		@RequestParam(required = false, defaultValue = "10") int requestSize,
		@Parameter(description = "마지막 다가오는 일정 수 (커서 용도)", in = ParameterIn.QUERY)
		@RequestParam(required = false) Long meetingCount
	)
	{
		// 활동이 활발한 모임 조회
		List<ActiveGroup> activeGroups = groupService.readMostActiveGroups(lastGroupId, meetingCount, requestSize);

		// 모임 ID 추출
		List<Long> groupIds = activeGroups.stream()
			.map(ActiveGroup::groupId)
			.toList();

		// 모임 관련 정보 조회
		List<ActiveGroupDetail> activeGroupDetails = groupService.readGroupsDetail(groupIds);

		// 현재 사용자와 모임들 관계 조회
		List<ActiveGroupRelation> activeGroupRelations = groupService.readGroupsRelation(groupIds);

		// List -> Map
		Map<Long, ActiveGroupDetail> detailMap = activeGroupDetails.stream().collect(Collectors.toMap(
			ActiveGroupDetail::groupId,
			Function.identity()
		));

		Map<Long, ActiveGroupRelation> relationMap = activeGroupRelations.stream().collect(Collectors.toMap(
			ActiveGroupRelation::groupId,
			Function.identity()
		));

		// 다음 페이지 유무
		boolean hasNext = CursorUtilClass.hasNext(activeGroups, requestSize);

		// 요청 크기에 맞게 가공
		List<ActiveGroup> extractContent =
			CursorUtilClass.extractContent(activeGroups, hasNext, requestSize);

		// 응답 DTO 변환
		List<GroupInfoResponseDto> response = extractContent.stream()
			.map(g -> GroupInfoResponseDto.of(g, detailMap.get(g.groupId()), relationMap.get(g.groupId()))
			).toList();

		// 응답 커서 추출
		ActiveGroupCursor cursor = ActiveGroupCursor.of(hasNext, response);

		return ResponseEntity.ok(ResponseHandler.response(CursorPageResponseDto.of(
			response,
			cursor
		)));
	}

	@Operation(
		summary = "인기: 내 주변 인기 모임 조회",
		description = "내 주변 인기 모임 조회"
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "내 주변 인기 모임 조회"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근")
	})
	@GetMapping("/v1/groups/nearby/popular")
	public ResponseEntity<ResponseHandler<CursorPageResponseDto<GroupInfoResponseDto, NearbyPopularGroupCursor>>> getNearbyPopularGroups(
		@Parameter(description = "마지막 모임 ID (커서 용도)", in = ParameterIn.QUERY)
		@RequestParam(required = false) Long lastGroupId,
		@Parameter(description = "페이지 크기 (고정 크기 = 10)", in = ParameterIn.QUERY)
		@RequestParam(required = false, defaultValue = "10") int requestSize,
		@Parameter(description = "마지막 회원 수 (커서 용도)", in = ParameterIn.QUERY)
		@RequestParam(required = false) Long memberCount
	)
	{
		// 인기 모임 조회
		List<PopularGroupSummary> summaries = groupService.readPopularGroupsNearMe(
			lastGroupId,
			requestSize,
			memberCount);

		// 모임 ID 추출
		List<Long> groupIds = summaries.stream()
			.map(PopularGroupSummary::groupId)
			.toList();

		// 모임별 추가 정보 조회
		List<PopularGroupRelation> commonInfos = groupService.readGroupsCommonInfo(groupIds);

		// ID -> PopularGroupRelation 매핑
		Map<Long, PopularGroupRelation> commonInfoMap = commonInfos.stream().collect(
			Collectors.toMap(PopularGroupRelation::groupId, Function.identity()));

		// 다음 페이지 유무
		boolean hasNext = CursorUtilClass.hasNext(summaries, requestSize);

		// 요청 크기에 맞게 가공
		List<PopularGroupSummary> extractSummaries =
			CursorUtilClass.extractContent(summaries, hasNext, requestSize);

		// 응답 DTO 변환
		List<GroupInfoResponseDto> response = extractSummaries.stream().map(
			s -> GroupInfoResponseDto.of(s, commonInfoMap.get(s.groupId())))
			.toList();

		// 응답 커서 추출
		NearbyPopularGroupCursor cursor = NearbyPopularGroupCursor.of(hasNext, response);

		return ResponseEntity.ok(ResponseHandler.response(CursorPageResponseDto.of(
			response,
			cursor
		)));
	}

	@Operation(
		summary = "모임 통계 조회",
		description = "모임 연간 일정, 모임 월간 일정"
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 통계 조회 성공"),
		@ApiResponse(
			responseCode = "401",
			description = "인증되지 않은 사용자 접근"),
		@ApiResponse(
			responseCode = "403",
			description = "현재 사용자가 해당 모임장이 아닌 경우"),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임")
	})
	@GetMapping("/v1/groups/{groupId}/statistics")
	public ResponseEntity<ResponseHandler<GroupStatisticsResponseDto>> getGroupStatistics(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId
	)
	{
		// 모임장 확인
		groupService.checkOwner(groupId);

		LocalDateTime now = LocalDateTime.now();
		Long annualSchedule = groupService.readAnnualScheduleCount(groupId, now);
		Long monthlySchedule = groupService.readMonthlyScheduleCount(groupId, now);

		return ResponseEntity.ok(ResponseHandler.response(
			GroupStatisticsResponseDto.of(
				annualSchedule,
				monthlySchedule
			)
		));
	}

	/**
	 * 내 모임 - 가입한 모임 조회
	 */
	@GetMapping("/v1/groups/joined")
	@Operation(
		summary = "내 모임 - 가입한 모임 조회",
		description = "내가 가입한 모임을 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CommonCursorPageResponseDto.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<ResponseHandler<CommonCursorPageResponseDto<GroupSummaryResponseDto>>> getJoinedGroups(
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 ID (첫 페이지는 생략, 이전 페이지의 nextCursorId)") Long cursorId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size
	) {
		CommonCursorPageResponseDto<GroupSummaryResponseDto> response = groupService.getJoinedGroups(cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	/**
	 * 홈/프로필 - 찜한 모임 조회
	 */
	@GetMapping("/v1/groups/liked")
	@Operation(
		summary = "홈/프로필 - 찜한 모임 조회",
		description = "내가 찜한 모임을 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CommonCursorPageResponseDto.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<ResponseHandler<CommonCursorPageResponseDto<GroupSummaryResponseDto>>> getLikedGroups(
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 ID (첫 페이지는 생략, 이전 페이지의 nextCursorId)") Long cursorId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size
	) {
		CommonCursorPageResponseDto<GroupSummaryResponseDto> response = groupService.getLikedGroups(cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	/**
	 * 홈 - 나와 비슷한 관심사 모임 조회
	 */
	@GetMapping("/v1/groups/recommend/category")
	@Operation(
		summary = "홈 - 나와 비슷한 관심사 모임 조회",
		description = "나와 비슷한 관심사의 모임을 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CommonCursorPageResponseDto.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<ResponseHandler<CommonCursorPageResponseDto<GroupSummaryResponseDto>>> getRecommendedGroupsByCategory(
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 ID (첫 페이지는 생략, 이전 페이지의 nextCursorId)") Long cursorId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size
	) {
		CommonCursorPageResponseDto<GroupSummaryResponseDto> response = groupService.getRecommendedGroupsByCategory(cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	/**
	 * 홈 - 나와 가까운 모임 조회
	 */
	@GetMapping("/v1/groups/recommend/location")
	@Operation(
		summary = "홈 - 나와 가까운 모임 조회",
		description = "나와 가까운 모임을 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = CommonCursorPageResponseDto.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<ResponseHandler<CommonCursorPageResponseDto<GroupSummaryResponseDto>>> getRecommendedGroupsByLocation(
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 ID (첫 페이지는 생략, 이전 페이지의 nextCursorId)") Long cursorId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size
	) {
		CommonCursorPageResponseDto<GroupSummaryResponseDto> response = groupService.getRecommendedGroupsByLocation(cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}
}
