package com.onmoim.server.chat.service;

import java.time.LocalDateTime;

import com.onmoim.server.chat.service.retry.ChatMessageRetryService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.dto.ChatUserDto;
import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.enums.DeliveryStatus;
import com.onmoim.server.chat.domain.enums.MessageType;
import com.onmoim.server.chat.domain.enums.SubscribeRegistry;
import com.onmoim.server.chat.repository.ChatMessageRepository;
import com.onmoim.server.chat.repository.RoomChatMessageIdGenerator;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
	private final ChatMessageRepository chatMessageRepository;
	private final RoomChatMessageIdGenerator roomChatMessageIdGenerator;
	private final SimpMessagingTemplate messagingTemplate;
	private final ChatMessageService chatMessageService;
	private final ChatMessageRetryService chatMessageRetryService;

	/**
	 * 시스템 메시지 전송
	 */
	@Transactional
	public ChatRoomMessageId sendSystemMessage(Long roomId, String content) {
		ChatRoomMessage systemMessage = ChatRoomMessage.create(
			ChatRoomMessageId.create(roomId, roomChatMessageIdGenerator.getSequence(roomId)),
			null,
			content,
			LocalDateTime.now(),
			MessageType.SYSTEM,
			DeliveryStatus.PENDING
		);

		// 데이터베이스에 저장
		chatMessageRepository.save(systemMessage);

		// 시스템 메시지 브로드캐스트
		// com.onmoim.server.chat.service.ChatMessageEventHandler 처리
		String destination = SubscribeRegistry.CHAT_ROOM_SUBSCRIBE_PREFIX.getDestination() + roomId;
		handleMessageSend(
				destination,
				ChatMessageDto.of(systemMessage, ChatUserDto.createSystem())
		);

		log.debug("시스템 메시지 전송 완료: 방ID: {}, 내용: {}", roomId, content);
		return systemMessage.getId();
	}

	@Transactional
	public void sendUserMessage(ChatMessageDto message) {
		Long roomId = message.getRoomId();

		ChatRoomMessage chatRoomMessage = ChatRoomMessage.create(
			ChatRoomMessageId.create(roomId, roomChatMessageIdGenerator.getSequence(roomId)),
			message.getSenderId(),
			message.getContent(),
			message.getTimestamp(),
			message.getType(),
			DeliveryStatus.PENDING
		);

		chatMessageRepository.save(chatRoomMessage);

		String destination = SubscribeRegistry.CHAT_ROOM_SUBSCRIBE_PREFIX.getDestination() + roomId;
		handleMessageSend(
			destination, ChatMessageDto.of(chatRoomMessage, message.getChatUserDto())
		);

	}

	/**
	 * 메시지 전송 상태 업데이트
	 */
	@Transactional
	public void updateMessageDeliveryStatus(ChatRoomMessageId messageId, DeliveryStatus status) {
		ChatRoomMessage message = chatMessageRepository.findById(messageId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_MESSAGE));

		message.setDeliveryStatus(status);
		chatMessageRepository.save(message);
		log.debug("메시지 상태 업데이트: ID: {}, 상태: {}", messageId, status);
	}

	public void handleMessageSend(String destination, ChatMessageDto message) {
		ChatRoomMessageId messageId = ChatRoomMessageId.create(message.getRoomId(),message.getMessageSequence());

		try {
			// WebSocket을 통해 메시지 전송
			messagingTemplate.convertAndSend(destination, message);

			// 전송 성공 시 SENT 상태 업데이트
			chatMessageService.updateMessageDeliveryStatus(messageId, DeliveryStatus.SENT);
			log.debug("메시지 전송 완료: ID: {}, 방ID: {}", messageId, message.getRoomId());

		} catch (Exception e) {
			// 전송 실패 시 FAILED 상태 업데이트
			chatMessageService.updateMessageDeliveryStatus(messageId, DeliveryStatus.FAILED);
			log.warn("메시지 전송 실패: ID: {}, 방ID: {}, 오류: {}", messageId, message.getRoomId(), e.getMessage());

			// 실패 재시도 처리
			chatMessageRetryService.failedProcess(message, destination);
		}
	}
}
