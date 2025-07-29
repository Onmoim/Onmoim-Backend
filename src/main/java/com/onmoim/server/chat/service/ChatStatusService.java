package com.onmoim.server.chat.service;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.enums.DeliveryStatus;
import com.onmoim.server.chat.repository.ChatMessageRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStatusService {
	private final ChatMessageRepository chatMessageRepository;

	/**
	 * 메시지 전송 상태 업데이트
	 */
	public void updateMessageDeliveryStatus(ChatRoomMessageId messageId, DeliveryStatus status) {
		ChatRoomMessage message = chatMessageRepository.findById(messageId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_MESSAGE));

		message.setDeliveryStatus(status);
		chatMessageRepository.save(message);
		log.debug("메시지 상태 업데이트: ID: {}, 상태: {}", messageId, status);
	}
}
