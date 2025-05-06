package com.onmoim.server.oauth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.oauth.dto.OAuthRequest;
import com.onmoim.server.oauth.dto.OAuthResponse;
import com.onmoim.server.oauth.dto.ReissueTokenRequest;
import com.onmoim.server.oauth.service.OAuthService;
import com.onmoim.server.user.dto.SignupRequest;
import com.onmoim.server.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth", description = "Auth 관련 API")
public class AuthController {

	private final OAuthService oAuthService;
	private final UserService userService;

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
	public ResponseEntity<ResponseHandler<OAuthResponse>> login(@RequestBody OAuthRequest oAuthRequest) {
		log.info("im here");
		OAuthResponse response = oAuthService.login(oAuthRequest.getProvider(), oAuthRequest.getToken());
		log.info("response = {}", response);

		String status = response.getStatus();

		if (status.equals("EXISTS")) {
			return ResponseEntity.ok(ResponseHandler.response(response));
		} else {
			// TODO: 프론트와 상의 후 return 값 수정 필요
			return ResponseEntity.ok(ResponseHandler.errorResponse(response, "ERROR"));
		}
	}


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
			responseCode = "500",
			description = "서버 오류 - 이미 가입된 유저거나 DB 오류 발생"
		)
	})
	public ResponseEntity<ResponseHandler<String>> signup(@RequestBody SignupRequest signupRequest) {
		userService.signup(signupRequest);
		return ResponseEntity.ok(ResponseHandler.response("회원가입이 정상적으로 완료되었습니다."));
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
		)
		// TODO: responseHandler 수정해서 401 띄우기
		// @ApiResponse(
		// 	responseCode = "500",
		// 	description = "서버 오류 - 이미 가입된 유저거나 DB 오류 발생"
		// )
	})
	public ResponseEntity<ResponseHandler<OAuthResponse>> reissueAccessToken(@RequestBody ReissueTokenRequest reissueTokenRequest) {
		OAuthResponse response = oAuthService.reissueAccessToken(reissueTokenRequest.getRefreshToken());
		return ResponseEntity.ok(ResponseHandler.response(response));
	}

}
