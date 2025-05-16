package com.onmoim.server.chat.exception;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;

/**
 * STOMP-웹소켓 에외 처리를 위한 이벤트
 */
@Getter
public class StompErrorEvent extends ApplicationEvent {

	private final String userIdOrSessionId;
	private final String destination;
	private final String errorMessage;

	public StompErrorEvent(Object source, String userIdOrSessionId, String destination, String errorMessage) {
		super(source);
		this.userIdOrSessionId = userIdOrSessionId;
		this.destination = destination;
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
			.append("userIdOrSessionId", userIdOrSessionId)
			.append("destination", destination)
			.append("errorMessage", errorMessage)
			.toString();
	}
}

