package com.onmoim.server.chat.facade;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.dto.ChatMessageDto;
import com.onmoim.server.chat.service.ChatMessageService;
import com.onmoim.server.chat.service.ChatRoomMemberQueryService;
import com.onmoim.server.user.service.UserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 메시지 관련 비즈니스 로직을 담당하는 Facade
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageFacade {

	private final ChatRoomMemberQueryService chatRoomMemberQueryService;
	private final UserQueryService userQueryService;
	private final ChatMessageService chatMessageService;

	/**
	 * 메시지 전송
	 */
	@Transactional
	public void sendMessage(ChatMessageDto message, Long userId) {
		Long roomId = message.getRoomId();
		log.debug("messageDto : {}, sender : {}",message,userId);

		chatRoomMemberQueryService.getByChatRoomIdAndUserId(roomId, userId);

		// 메시지에 인증된 사용자 ID 설정
		// User user = userQueryService.findById(userId);
		message.setSenderId(userId);
		message.setSenderName("name");

		// 메시지 전송 서비스 호출
		chatMessageService.sendMessage(message);
	}

}

