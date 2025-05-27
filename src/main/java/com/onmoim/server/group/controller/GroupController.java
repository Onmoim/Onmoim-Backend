package com.onmoim.server.group.controller;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.group.dto.request.CreateGroupRequestDto;
import com.onmoim.server.group.dto.request.TransferOwnerRequestDto;
import com.onmoim.server.group.dto.response.CursorPageResponseDto;
import com.onmoim.server.group.service.GroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
	public ResponseEntity<ResponseHandler<Long>> createGroup(
		@RequestBody @Valid CreateGroupRequestDto request
	)
	{
		Long groupId = groupService.createGroup(request);
		return ResponseEntity
			.status(CREATED)
			.body(ResponseHandler.response(groupId));
	}

	@Operation(
		summary = "모임 가입",
		description = "모임을 가입합니다. 모임 가입 성공 시 '모임 가입 성공' 메시지가 반환됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "201",
			description = "모임 가입 성공"),
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
	public ResponseEntity<ResponseHandler<String>> joinGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId
	)
	{
		groupService.joinGroup(groupId);
		return ResponseEntity
			 .status(CREATED)
			 .body(ResponseHandler.response("모임 가입 성공"));
	}

	@Operation(
		summary = "모임 좋아요(찜)",
		description = "좋아요 또는 좋아요 취소로 동작합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 좋아요 또는 좋아요 취소 성공"),
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
			responseCode = "429",
			description = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.")
	})
	@PostMapping("/v1/groups/{groupId}/like")
	public ResponseEntity<ResponseHandler<Void>> likeGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId
	)
	{
		groupService.likeGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response(null));
	}

	@Operation(
		summary = "모임 회원 조회",
		description = "모임에 가입한 회원들을 커서 기반 페이징으로 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 조회 성공",
			content = @Content(
				schema = @Schema(implementation = CursorPageResponseDto.class))),
		@ApiResponse(
			responseCode = "404",
			description = "존재하지 않는 모임")
	})
	@GetMapping("/v1/groups/{groupId}/members")
	public ResponseEntity<ResponseHandler<?>> getGroupMembers(
		@Parameter(description = "모임 ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId,
		@Parameter(description = "커서 ID (마지막 조회한 커서 ID)", in = ParameterIn.QUERY)
		@RequestParam(required = false) Long cursorId,
		@Parameter(description = "페이지 크기 (고정 크기 = 10)", in = ParameterIn.QUERY)
		@RequestParam(required = false, defaultValue = "10") int size
	)
	{
		var response = groupService.getGroupMembers(groupId, cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
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
	public ResponseEntity<ResponseHandler<?>> leaveGroup(
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
	@PatchMapping("v1/groups/{groupId}/owner")
	public ResponseEntity<ResponseHandler<String>> transferOwnership(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId,
		@Valid @RequestBody TransferOwnerRequestDto request
	)
	{
		groupService.transferOwnership(groupId, request.getMemberId());
		return ResponseEntity.ok(ResponseHandler.response("모임장 변경 성공"));
	}
}
