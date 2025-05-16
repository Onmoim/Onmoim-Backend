package com.onmoim.server.group.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.group.dto.request.CreateGroupRequestDto;
import com.onmoim.server.group.service.GroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class GroupController {
	private final GroupService groupService;

	@PostMapping("/v1/groups")
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
			description = "모임 생성 실패 - 이유: 이미 존재하는 모임 이름, 잘못된 정원 설정(최소:5, 최대:300), 요청 누락")})
	public ResponseEntity<ResponseHandler<?>> createGroup(
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
			description = "모임 가입 실패 - 이유: 이미 가입된 모임, 모임에서 벤 당한 상태")})
	@PostMapping("/v1/groups/{groupId}/join")
	public ResponseEntity<ResponseHandler<?>> joinGroup(@PathVariable Long groupId) {
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
			description = "이미 가입된 회원 또는 모임에서 벤 당한 회원")})
	@PostMapping("/v1/groups/{groupId}/like")
	public ResponseEntity<ResponseHandler<?>> likeGroup(@PathVariable Long groupId) {
		groupService.likeGroup(groupId);
		return ResponseEntity.ok(ResponseHandler.response(null));
	}
}
