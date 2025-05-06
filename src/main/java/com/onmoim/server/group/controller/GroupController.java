package com.onmoim.server.group.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.group.request.CreateGroupRequest;
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
					@RequestBody @Valid CreateGroupRequest request) {
		Long groupId = groupService.createGroup(request);
		return ResponseEntity.ok(ResponseHandler.response(groupId));
	}
}
