package com.onmoim.server.common.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.onmoim.server.common.response.Message;
import com.onmoim.server.common.response.ResponseHandler;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	@ExceptionHandler(value = CustomException.class)
	public ResponseEntity<?> handleCustomException(CustomException exception) {
		log.warn("[handleCustomException] : {} \n message: {}", exception.getErrorCode(),
			exception.getMessage());

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ResponseHandler.errorResponse(exception.getMessage(), exception.getErrorCode().name()));
	}

	@ExceptionHandler(value = MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleMethodArgumentException(MethodArgumentNotValidException exception) {
		List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors()
			.stream().map(FieldErrorResponse::of)
			.collect(Collectors.toList());

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ResponseHandler.errorResponse(errors, Message.BAD_REQUEST.getDescription()));
	}

	@ExceptionHandler(value = MaxUploadSizeExceededException.class)
	public ResponseEntity<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
		log.warn("[handleMaxUploadSizeExceeded] : {}", exception.getMessage());

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ResponseHandler.errorResponse(
				ErrorCode.FILE_SIZE_EXCEEDED.getDetail(),
				ErrorCode.FILE_SIZE_EXCEEDED.name()));
	}

	@ExceptionHandler(value = Exception.class)
	public ResponseEntity<?> handleServerException(Exception exception) {
		// TODO: ServerError log message
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ResponseHandler.errorResponse(exception.getMessage(), Message.SYSTEM_ERROR.getDescription()));
	}

	private record FieldErrorResponse(String field, String message, Object rejectedValue) {
		public static FieldErrorResponse of(final FieldError fieldError) {
			return new FieldErrorResponse(
				fieldError.getField(),
				fieldError.getDefaultMessage(),
				fieldError.getRejectedValue()
			);
		}
	}
}
