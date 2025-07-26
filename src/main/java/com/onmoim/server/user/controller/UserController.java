package com.onmoim.server.user.controller;

import com.onmoim.server.group.dto.response.RecentViewedGroupSummaryResponseDto;
import com.onmoim.server.group.dto.response.cursor.RecentViewCursorPageResponseDto;
import com.onmoim.server.group.service.GroupService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.user.dto.request.CreateUserCategoryRequestDto;
import com.onmoim.server.user.dto.request.SignupRequestDto;
import com.onmoim.server.user.dto.request.UpdateProfileRequestDto;
import com.onmoim.server.user.dto.response.ProfileResponseDto;
import com.onmoim.server.user.dto.response.SignupResponseDto;
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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User", description = "User 관련 API")
public class UserController {

	private final UserService userService;
	private final GroupService groupService;

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
	public ResponseEntity<ResponseHandler<SignupResponseDto>> signup(@RequestBody @Valid SignupRequestDto request) {
		SignupResponseDto response = userService.signup(request);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	@PutMapping(value = "/profile/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(
		summary = "프로필 수정",
		description = "로그인한 유저의 프로필을 편집합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "프로필 수정 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ResponseHandler.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "BAD REQUEST - 요청값 오류 발생"
		)
	})
	public ResponseEntity<ResponseHandler<String>> updateUserProfile(
		@PathVariable Long id,
		@Valid @RequestPart("request") UpdateProfileRequestDto request,
		@RequestPart(value = "image", required = false) MultipartFile profileImgFile
	) {
		userService.updateUserProfile(id, request, profileImgFile);
		return ResponseEntity.ok(ResponseHandler.response("프로필 수정이 완료되었습니다."));
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

	@DeleteMapping("/{id}")
	@Operation(
		summary = "회원 탈퇴",
		description = "회원이 서비스를 탈퇴합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "회원 탈퇴 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ResponseHandler.class)
			)
		),
		@ApiResponse(
			responseCode = "409",
			description = "CONFLICT - 비즈니스 로직 오류"
		)
	})
	public ResponseEntity<ResponseHandler<String>> leaveUser(@PathVariable Long id) {
		userService.leaveUser(id);
		return ResponseEntity.ok(ResponseHandler.response("회원 탈퇴가 완료되었습니다."));
	}

	/**
	 * 프로필 - 최근 본 모임 조회
	 */
	@GetMapping("/recent/groups")
	@Operation(
		summary = "최근 본 모임 조회",
		description = "최근 본 모임을 조회합니다.")
	@ApiResponses({
		@ApiResponse(
			responseCode = "200",
			description = "조회 성공",
			content = @Content(
				schema = @Schema(implementation = RecentViewCursorPageResponseDto.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	public ResponseEntity<ResponseHandler<RecentViewCursorPageResponseDto<RecentViewedGroupSummaryResponseDto>>> getRecentViewedGroups(
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 조회 시각 (첫 페이지는 생략, 이전 페이지의 nextCursorViewedAt)") LocalDateTime cursorViewedAt,
		@RequestParam(required = false)
		@Parameter(description = "다음 페이지 커서 ID (첫 페이지는 생략, 이전 페이지의 nextCursorId)") Long cursorId,
		@RequestParam(defaultValue = "10")
		@Parameter(description = "페이지 크기") int size
	) {
		RecentViewCursorPageResponseDto<RecentViewedGroupSummaryResponseDto> response = groupService.getRecentViewedGroups(cursorViewedAt, cursorId, size);
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

}
