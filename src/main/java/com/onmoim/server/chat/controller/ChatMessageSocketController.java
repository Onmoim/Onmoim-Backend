package com.onmoim.server.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.onmoim.server.chat.dto.ChatMessageDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatMessageSocketController {

	/**
	 * 채팅방에 메시지 전송
	 * 클라이언트: /app/chat.sendMessage/{roomId} 형식으로 요청
	 */
	@MessageMapping("/chat.sendMessage/{roomId}")
	public void sendMessage(
		@DestinationVariable String roomId,
		@Payload ChatMessageDto chatMessage,
		SimpMessageHeaderAccessor headerAccessor,
		Principal principal) {

		// Principal에서 사용자 ID 가져오기
		String userId = principal.getName();

		// 메시지에 방 ID 설정
		chatMessage.setRoomId(roomId);

		// Facade를 통한 메시지 전송
		chatMessageFacade.sendMessage(chatMessage, userId);
	}
}
