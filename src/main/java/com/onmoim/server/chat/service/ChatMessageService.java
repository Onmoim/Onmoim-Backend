package com.onmoim.server.chat.service;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.dto.ChatMessageDto;
import com.onmoim.server.chat.entity.ChatRoomMessage;
import com.onmoim.server.chat.entity.ChatRoomMessageId;
import com.onmoim.server.chat.entity.DeliveryStatus;
import com.onmoim.server.chat.entity.MessageType;
import com.onmoim.server.chat.entity.SubscribeRegistry;
import com.onmoim.server.chat.repository.ChatMessageRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
	private final ChatMessageRepository chatMessageRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final RoomChatMessageIdGenerator roomChatMessageIdGenerator;

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
		eventPublisher.publishEvent(new MessageSendEvent(destination, ChatMessageDto.of(systemMessage, "SYSTEM")));

		log.debug("시스템 메시지 전송 완료: 방ID: {}, 내용: {}", roomId, content);
		return systemMessage.getId();
	}

	@Transactional
	public void sendMessage(ChatMessageDto message) {
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
		eventPublisher.publishEvent(new MessageSendEvent(destination, ChatMessageDto.of(chatRoomMessage, message.getSenderName())));

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
}
