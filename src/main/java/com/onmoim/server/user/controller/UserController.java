package com.onmoim.server.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.user.dto.request.CreateUserCategoryRequestDto;
import com.onmoim.server.user.dto.request.SignupRequestDto;
import com.onmoim.server.user.dto.response.ProfileResponseDto;
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
	public ResponseEntity<ResponseHandler<Long>> signup(@RequestBody @Valid SignupRequestDto request) {
		Long userId = userService.signup(request);
		return ResponseEntity.ok(ResponseHandler.response(userId));
	}

	@PostMapping("/category")
	@Operation(
		summary = "관심사 설정",
		description = "회원가입 이후 관심사를 설정합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "관심사 설정 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ResponseHandler.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "BAD REQUEST - 카테고리값 오류 발생"
		)
	})
	public ResponseEntity<ResponseHandler<String>> createUserCategory(@RequestBody @Valid CreateUserCategoryRequestDto request) {
		userService.createUserCategory(request);
		return ResponseEntity.ok(ResponseHandler.response("관심사 설정이 완료되었습니다."));
	}

	@GetMapping("/profile")
	@Operation(
		summary = "내정보",
		description = "로그인한 유저의 프로필이 조회됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "프로필 조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ResponseHandler.class)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "BAD REQUEST - 사용자 인증 오류"
		)
	})
	public ResponseEntity<ResponseHandler<ProfileResponseDto>> getProfile() {
		ProfileResponseDto response = userService.getProfile();
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

}
