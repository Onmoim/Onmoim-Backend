package com.onmoim.server.chat.common.config;

import com.onmoim.server.chat.common.exception.StompErrorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageMappingInfo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;

import java.util.Set;

/**
 * 존재하지 않은 MessageMapping 경로가  들어왔을 때 예외 처리를 위한 CustomHandler
 * - 기본 'WebSocketAnnotationMethodMessageHandler' 에서는 잘못된 MessageMapping 경로를 입력했을 때, 예외발생 없이 로그만 출력됩니다.
 */
@Slf4j
public class CustomWebSocketAnnotationMethodMessageHandler extends WebSocketAnnotationMethodMessageHandler {

	private final ApplicationEventPublisher eventPublisher;

	public CustomWebSocketAnnotationMethodMessageHandler(SubscribableChannel clientInChannel,
		MessageChannel clientOutChannel, SimpMessageSendingOperations brokerTemplate,
		ApplicationEventPublisher eventPublisher) {
		super(clientInChannel, clientOutChannel, brokerTemplate);
		this.eventPublisher = eventPublisher;
	}

	@Override
	protected void handleNoMatch(Set<SimpMessageMappingInfo> simpMessageMappingInfos, String lookupDestination,
		Message<?> message) {
		super.handleNoMatch(simpMessageMappingInfos, lookupDestination, message);

		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		String userId = accessor.getUser().getName();
		log.debug("AnnotationMethodHandler HandleNoMatch ErrorEvent 발행 ");
		// 이벤트 발행
		eventPublisher.publishEvent(
			new StompErrorEvent(this, userId, lookupDestination, "지원하지 않는 경로입니다: " + lookupDestination));
	}
}
