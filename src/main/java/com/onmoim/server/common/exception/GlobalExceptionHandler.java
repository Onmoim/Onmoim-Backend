package com.onmoim.server.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.onmoim.server.common.response.ResponseHandler;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	@ExceptionHandler(value = CustomException.class)
	public ResponseEntity<?> handleCustomException(CustomException customException) {
		log.warn("[handleCustomException] : {} \n message: {}", customException.getErrorCode(),
			customException.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ResponseHandler.errorResponse(customException));
	}

	@ExceptionHandler(value = MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleMethodArgumentException(MethodArgumentNotValidException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ResponseHandler.errorResponse(exception));
	}

	@ExceptionHandler(value = Exception.class)
	public ResponseEntity<?> handleServerException(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ResponseHandler.errorResponse());
	}
}
