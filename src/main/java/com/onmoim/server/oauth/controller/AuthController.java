package com.onmoim.server.oauth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.oauth.dto.OAuthRequestDto;
import com.onmoim.server.oauth.dto.OAuthResponseDto;
import com.onmoim.server.oauth.dto.ReissueTokenRequestDto;
import com.onmoim.server.oauth.service.OAuthService;
import com.onmoim.server.oauth.service.RefreshTokenService;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "Auth 관련 API")
public class AuthController {

	private final OAuthService oAuthService;
	private final UserService userService;
	private final RefreshTokenService refreshTokenService;

	@PostMapping("/oauth")
	@Operation(
		summary = "소셜 로그인",
		description = "구글 또는 카카오 로그인 토큰을 받아 JWT를 발급합니다. 유저가 미가입 상태라면 상태값만 반환됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
				responseCode = "200",
				description = "로그인 성공 또는 미가입 상태",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseHandler.class)
				)
			),
		@ApiResponse(
				responseCode = "500",
				description = "서버 내부 오류"
			)
	})
	public ResponseEntity<ResponseHandler<OAuthResponseDto>> login(
				@Valid @RequestBody OAuthRequestDto oAuthRequestDto) {
		OAuthResponseDto response = oAuthService.login(oAuthRequestDto.getProvider(), oAuthRequestDto.getToken());

		return ResponseEntity.ok(ResponseHandler.response(response));
	}


	@PostMapping("/reissue-tkn")
	@Operation(
		summary = "Access Token 재발급",
		description = "Refresh Token을 통해 Access Token을 재발급합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
				responseCode = "200",
				description = "Access Token 재발급 성공",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseHandler.class)
				)
			),
		@ApiResponse(
			responseCode = "401",
			description = "UNAUTHORIZED - 유효하지 않은 refresh token, 일치하지 않는 refresh token, 존재하지 않는 사용자"
		)
	})
	public ResponseEntity<ResponseHandler<OAuthResponseDto>> reissueAccessToken(
		@RequestBody ReissueTokenRequestDto reissueTokenRequestDto) {
		OAuthResponseDto response = oAuthService.reissueAccessToken(reissueTokenRequestDto.getRefreshToken());
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

	@PostMapping("/logout")
	@Operation(
		summary = "로그아웃",
		description = "RefreshToken 제거 및 로그아웃 처리"
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "로그아웃 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ResponseHandler.class)
			)
		),
		@ApiResponse(
			responseCode = "401",
			description = "UNAUTHORIZED - 인증 실패"
		)
	})
	public ResponseEntity<ResponseHandler<String>> logout() {
		Long userId = userService.getCurrentUserId();
		refreshTokenService.deleteRefreshToken(userId);

		return ResponseEntity.ok(ResponseHandler.response("로그아웃 성공"));
	}

}
