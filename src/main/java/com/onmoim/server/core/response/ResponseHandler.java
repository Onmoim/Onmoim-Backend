package com.onmoim.server.core.response;

import java.time.LocalDateTime;

import com.onmoim.server.core.exception.ErrorCode;

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
			.message("SUCCESS")
			.data(data)
			.build();
	}

	public static <T> ResponseHandler<T> errorResponse(final ErrorCode errorCode) {
		return ResponseHandler.<T>builder()
			.message(errorCode.name())
			.data((T)errorCode.getDetail())
			.build();
	}
}
