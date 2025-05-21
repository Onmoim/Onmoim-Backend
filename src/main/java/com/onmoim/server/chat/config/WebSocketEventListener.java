package com.onmoim.server.chat.config;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.onmoim.server.chat.dto.ChatMessage;
import com.onmoim.server.chat.exception.StompErrorEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

	private final SimpMessagingTemplate messagingTemplate;

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectedEvent event) {
		log.debug("Received a new web socket connection");
	}

	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		log.debug("WebSocket connection disconnected");
	}

	@EventListener
	public void onSubscribe(SessionSubscribeEvent event) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
		log.debug("구독됨: sessionId={}, user={}, destination={}, subscriptionId={}", accessor.getSessionId(),
			accessor.getUser() != null ? accessor.getUser().getName() : "null", accessor.getDestination(),
			accessor.getSubscriptionId());
	}

	/**
	 * 에외는 Event를 통해 처리되도록 구성했습니다.
	 * Controller 외부에서 발생하는 예외를 포함 모든 예외 정보 발송을 이곳에서 처리하도록 구성했습니다.
	 */
	@EventListener
	public void onError(StompErrorEvent event) {
		log.debug("Websocket StompErrorEvent 수신");
		System.out.println(event);

		ChatMessage errorMessage = ChatMessage.builder()
			.messageId(UUID.randomUUID().toString())
			.senderId("SYSTEM")
			.type(ChatMessage.MessageType.ERROR)
			.content(event.getErrorMessage())
			.timestamp(LocalDateTime.now())
			.build();

		messagingTemplate.convertAndSendToUser(event.getUserIdOrSessionId(), "/queue/error", errorMessage);
	}
}
