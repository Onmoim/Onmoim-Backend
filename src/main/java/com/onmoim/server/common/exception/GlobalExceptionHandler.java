package com.onmoim.server.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.onmoim.server.common.response.ResponseHandler;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	@ExceptionHandler(value = CustomException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<?> handleCustomException(CustomException customException) {
		log.warn("[handleCustomException] : {} \n message: {}", customException.getErrorCode(),
			customException.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ResponseHandler.errorResponse(customException));
	}
}
