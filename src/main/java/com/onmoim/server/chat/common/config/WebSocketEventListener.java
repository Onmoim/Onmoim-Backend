package com.onmoim.server.chat.common.config;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.dto.ChatUserDto;
import com.onmoim.server.chat.domain.enums.MessageType;
import com.onmoim.server.chat.domain.enums.SubscribeRegistry;
import com.onmoim.server.chat.common.exception.StompErrorEvent;

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
		send(MessageType.SUCCESS, "Destination : " + accessor.getDestination() + ", 구독 완료",
			accessor.getUser().getName());

		log.debug("구독됨: user={}, destination={}, subscriptionId={}", accessor.getUser(), accessor.getDestination(),
			accessor.getSubscriptionId());

	}

	@EventListener
	public void unSubscribe(SessionUnsubscribeEvent event) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
		send(MessageType.SUCCESS, "Destination : " + accessor.getDestination() + ", 구독 취소",
			accessor.getUser().getName());

		log.debug("구독 취소됨: user={}, destination={}, subscriptionId={}", accessor.getUser(), accessor.getDestination(),
			accessor.getSubscriptionId());

	}

	/**
	 * 에외는 Event를 통해 처리되도록 구성했습니다.
	 * Controller 외부에서 발생하는 예외를 포함 모든 예외 정보 발송을 이곳에서 처리하도록 구성했습니다.
	 */
	@EventListener
	public void onError(StompErrorEvent event) {
		log.debug("Websocket StompErrorEvent 수신");

		ChatMessageDto errorMessage = ChatMessageDto.builder()
			.chatUserDto(ChatUserDto.createSystem())
			.type(MessageType.ERROR)
			.content(event.getErrorMessage())
			.timestamp(LocalDateTime.now())
			.build();

		String destination = SubscribeRegistry.ERROR_SUBSCRIBE_DESTINATION.getDestination();
		messagingTemplate.convertAndSendToUser(event.getUserIdOrSessionId(), destination, errorMessage);
	}

	private void send(MessageType messageType, String content, String userId) {
		ChatMessageDto errorMessage = ChatMessageDto.builder()
			.chatUserDto(ChatUserDto.createSystem())
			.type(messageType)
			.content(content)
			.timestamp(LocalDateTime.now())
			.build();

		String destination = SubscribeRegistry.ERROR_SUBSCRIBE_DESTINATION.getDestination();
		messagingTemplate.convertAndSendToUser(userId, destination, errorMessage);
	}
}
