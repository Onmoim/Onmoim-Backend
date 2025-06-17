package com.onmoim.server.chat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.onmoim.server.chat.dto.ChatMessageDto;
import com.onmoim.server.chat.facade.ChatMessageFacade;
import com.onmoim.server.security.CustomUserDetails;

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
	 *
	 * {"messageSequence":3,"roomId":4,"groupId":null,"type":"CHAT","content":"안녕하세요! 오늘 날씨가 정말 좋네요.","senderId":102,"chatUserDto":{"id":102,"username":"홍석준","profileImageUrl":null,"owner":true},"timestamp":[2025,6,12,22,21,38,520926000]}
	 */
	@MessageMapping("/chat.sendMessage")
	public void sendMessage(
		@Payload ChatMessageDto chatMessage,
		Principal principal) {
		System.out.println("ChatMessageSocketController.sendMessage");
		// Long userId = getCurrentUserId();
		Long userId = 102L;
		chatMessage.setSenderId(userId);
		// Facade를 통한 메시지 전송
		chatMessageFacade.sendMessage(chatMessage, userId);
	}

	private Long getCurrentUserId() {
		CustomUserDetails principal = (CustomUserDetails)SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();
		return principal.getUserId();
	}
}
