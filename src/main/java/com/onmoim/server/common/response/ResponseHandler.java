package com.onmoim.server.common.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.onmoim.server.common.exception.CustomException;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ResponseHandler<T> {
	private final LocalDateTime timestamp = LocalDateTime.now();
	private String message;
	private T data;

	public ResponseHandler(final String message, T data) {
		this.message = message;
		this.data = data;
	}

	public static <T> ResponseHandler<T> response(T data) {
		return ResponseHandler.<T>builder()
			.message(Message.SUCCESS.message)
			.data(data)
			.build();
	}

	public static <T> ResponseHandler<T> errorResponse(final CustomException customException) {
		return ResponseHandler.<T>builder()
			.message(customException.getErrorCode().name())
			.data((T)customException.getMessage())
			.build();
	}

	public static <T> ResponseHandler<T> errorResponse(final MethodArgumentNotValidException exception) {
		List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors()
			.stream().map(FieldErrorResponse::of)
			.collect(Collectors.toList());

		return ResponseHandler.<T>builder()
			.message(Message.BAD_REQUEST.message)
			.data((T)errors)
			.build();
	}

	public static <T> ResponseHandler<T> errorResponse() {
		return ResponseHandler.<T>builder()
			.message(Message.SYSTEM_ERROR.message)
			.build();
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

	private enum Message {
		SUCCESS("SUCCESS"),
		BAD_REQUEST("BAD REQUEST"),
		SYSTEM_ERROR("SYSTEM ERROR");

		private final String message;

		Message(String message) {
			this.message = message;
		}
	}
}
