package com.onmoim.server.example.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.example.entity.TestEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("checkstyle:RegexpSingleline")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/response")
@Tag(name = "Test", description = "Test 관련 API입니다.")
public class ResponseController {

	/**
	 * @return
	 * {
	 * 	   "timestamp": "2025-04-04T03:41:20.833333",
	 *     "message": "DENIED_UNAUTHORIZED_USER",
	 *     "data": "로그인되지 않은 유저의 접근입니다."
	 * }
	 */

	@GetMapping("/exception")
	@Operation(summary = "예외 발생시 응답", description = "예외 발생시 응답을 확일할 수 있습니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "401", description = "DENIED_UNAUTHORIZED_USER 발생")})
	public ResponseEntity<String> testException() {
		log.info("예외 테스트 컨트롤러 실행");
		throw new CustomException(ErrorCode.DENIED_UNAUTHORIZED_USER);
	}

	/**
	 * @return
	 *{
	 *     "timestamp": "2025-04-09T22:03:35.929197",
	 *     "message": "DENIED_UNAUTHORIZED_USER",
	 *     "data": "상황에 따른 메시지 커스텀"
	 * }
	 */
	@GetMapping("/exception/message-custom")
	@Operation(summary = "예외 발생시 응답", description = "예외 발생시 응답을 확일할 수 있습니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "401", description = "DENIED_UNAUTHORIZED_USER 발생")})
	public ResponseEntity<String> testExceptionMessageCustom() {
		log.info("예외 테스트 컨트롤러 실행");
		throw new CustomException(ErrorCode.DENIED_UNAUTHORIZED_USER, "상황에 따른 메시지 커스텀");
	}

	/**
	 * @return
	 * {
	 * 	   "timestamp": "2025-04-04T03:41:20.833333",
	 *     "message": "SUCCESS",
	 *     "data": {
	 *         "id": 1,
	 *         "name": "hong",
	 *         "email": "email@naver.com"
	 *     }
	 * }
	 */
	@GetMapping("/success")
	@Operation(summary = "요청 성공시 응답", description = "요청 성공시 응답을 확인할 수 있습니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공")})
	public ResponseEntity<ResponseHandler<TestEntity>> testSuccess() {
		log.info("예외 테스트 컨트롤러 실행");
		TestEntity testEntity = new TestEntity();
		testEntity.setId(1L);
		testEntity.setEmail("email@naver.com");
		testEntity.setName("hong");
		return ResponseEntity.status(HttpStatus.OK).body(ResponseHandler.response(testEntity));
	}
}
