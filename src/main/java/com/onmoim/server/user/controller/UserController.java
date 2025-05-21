package com.onmoim.server.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.user.dto.SignupRequest;
import com.onmoim.server.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User 관련 API")
public class UserController {

	private final UserService userService;

	@PostMapping("/signup")
	@Operation(
		summary = "회원가입",
		description = "소셜 로그인 후 미가입 상태인 유저가 추가 정보를 입력하여 회원가입을 완료합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "회원가입 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ResponseHandler.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "BAD REQUEST - 이미 가입된 유저거나 요청값 오류 발생"
		)
	})
	public ResponseEntity<ResponseHandler<String>> signup(@RequestBody @Valid SignupRequest signupRequest) {
		userService.signup(signupRequest);
		return ResponseEntity.ok(ResponseHandler.response("회원가입이 정상적으로 완료되었습니다."));
	}

}
