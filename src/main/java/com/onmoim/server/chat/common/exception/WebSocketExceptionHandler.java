package com.onmoim.server.chat.common.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.security.Principal;

/**
 * Controller 이하 계층에서 발생 된 에러를 처리합니다.
 */

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class WebSocketExceptionHandler {

	private final ApplicationEventPublisher eventPublisher;

	/**
	 * STOMP 핸들러에서 발생한 예외를 처리합니다.
	 */
	@MessageExceptionHandler
	public void handleException(Exception exception, Message<?> message) {
		StompHeaderAccessor headerAccessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		Principal principal = headerAccessor.getUser();
		String destination = headerAccessor.getDestination();

		String userId = principal != null ? principal.getName() : "Unknown";

		log.error("WebSocket 메시지 처리 중 오류 발생 - 사용자: {}, 대상: {}, 오류: {}",
			userId, destination, exception.getMessage(), exception);

		eventPublisher.publishEvent(new StompErrorEvent(
			this,
			userId,
			destination,
			exception.getMessage()
		));
	}
}
