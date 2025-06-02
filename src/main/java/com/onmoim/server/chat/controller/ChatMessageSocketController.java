package com.onmoim.server.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.onmoim.server.chat.dto.ChatMessageDto;
import com.onmoim.server.chat.facade.ChatMessageFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatMessageSocketController {

	private final ChatMessageFacade chatMessageFacade;

	/**
	 * 채팅방에 메시지 전송
	 * 클라이언트: /app/chat.sendMessage 형식으로 요청
	 */
	@MessageMapping("/chat.sendMessage")
	public void sendMessage(
		@Payload ChatMessageDto chatMessage,
		Principal principal) {

		Long userId = Long.parseLong(principal.getName());
		chatMessage.setSenderId(userId);
		// Facade를 통한 메시지 전송
		chatMessageFacade.sendMessage(chatMessage, userId);
	}
}
