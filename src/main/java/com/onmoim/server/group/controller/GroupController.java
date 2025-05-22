package com.onmoim.server.group.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.group.dto.request.CreateGroupRequestDto;
import com.onmoim.server.group.dto.request.TransferOwnerRequestDto;
import com.onmoim.server.group.service.GroupService;

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

	@Operation(
		summary = "모임 생성",
		description = "모임을 생성합니다. 모임 생성 성공 시 생성된 모임 ID가 반환됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 생성 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "이미 존재하는 모임 이름, 잘못된 정원 설정(최소:5, 최대:300), 요청 누락")})
	@PostMapping("/v1/groups")
	public ResponseEntity<ResponseHandler<Long>> createGroup(
		@RequestBody @Valid CreateGroupRequestDto request) {
		Long groupId = groupService.createGroup(request);
		return ResponseEntity.ok(ResponseHandler.response(groupId));
	}

	@Operation(
		summary = "모임 가입",
		description = "모임을 가입합니다. 모임 가입 성공 시 모임 가입 성공 메시지가 반환됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 가입 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "존재하지 않는 모임, 존재하지 않는 유저, 이미 가입된 모임, 모임에서 벤 당한 상태")})
	@PostMapping("/v1/groups/{groupId}/join")
	public ResponseEntity<ResponseHandler<String>> joinGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId) {
		groupService.joinGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response("모임 가입 성공"));
	}

	@Operation(
		summary = "모임 좋아요(찜)",
		description = "좋아요 또는 좋아요 취소로 동작합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 좋아요 성공 또는 취소 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "존재하지 않는 모임, 이미 가입된 회원 또는 모임에서 벤 당한 회원")})
	@PostMapping("/v1/groups/{groupId}/like")
	public ResponseEntity<ResponseHandler<Void>> likeGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId) {
		groupService.likeGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response(null));
	}

	/**
	 * 체크리스트
	 * - 모임 커서 페이징 필요 유무 확인
	 * - 유저 이미지 분리? 현재는 유저 엔티티에서 관리
	 */
	@Operation(
		summary = "모임 회원 조회",
		description = "모임에 가입한 회원들을 조회합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 조회 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "존재하지 않는 모임")})
	@Deprecated
	@GetMapping("/v1/groups/{groupId}/members")
	public ResponseEntity<ResponseHandler<?>> getGroupMembers(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId) {
		return ResponseEntity.ok(ResponseHandler.response(groupService.getGroupMembers(groupId)));
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
			responseCode = "400",
			description = "존재하지 않는 모임, 권한 부족(모임장만 삭제 가능)")})
	@DeleteMapping("/v1/groups/{groupId}")
	public ResponseEntity<ResponseHandler<String>> deleteGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId) {
		groupService.deleteGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response("모임 삭제 성공"));
	}

	@Operation(
		summary = "모임 탈퇴",
		description = "모임에서 탈퇴합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임 탈퇴 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "존재하지 않는 모임, 모임장은 바로 탈퇴 불가능")})
	@DeleteMapping("/v1/groups/{groupId}/member")
	public ResponseEntity<ResponseHandler<?>> leaveGroup(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId) {
		groupService.leaveGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response("모임 탍퇴 성공"));
	}

	@Operation(
		summary = "모임장 변경",
		description = "모임장을 변경합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "모임장 변경 성공"),
		@ApiResponse(
			responseCode = "400",
			description = "존재하지 않는 모임, 모임장 권한이 없음, 모임장 변경 대상자가 해당 모임의 회원이 아님")})
	@PatchMapping("v1/groups/{groupId}/owner")
	public ResponseEntity<ResponseHandler<String>> transferOwnership(
		@Parameter(description = "모임ID", required = true, in = ParameterIn.PATH)
		@PathVariable Long groupId,
		@Valid @RequestBody TransferOwnerRequestDto request) {
		groupService.transferOwnership(groupId, request.getMemberId());
		return ResponseEntity.ok(ResponseHandler.response("모임장 변경 성공"));
	}
}
