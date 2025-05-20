package com.onmoim.server.chat.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.entity.DeliveryStatus;
import com.onmoim.server.chat.entity.MessageType;
import com.onmoim.server.chat.entity.RoomChatMessage;
import com.onmoim.server.chat.repository.ChatMessageRepository;

@SpringBootTest
public class TestChatMessageServiceIntegrationTest {

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private ChatMessageService chatMessageService;

	@Autowired
	private RoomChatMessageIdGenerator roomChatMessageIdGenerator;



	@Test
	@DisplayName("sendSystemMessage에서 DB 저장이 잘 되는지 테스트")
	@Transactional
	void testSendSystemMessageSave() {
		// given
		Long roomId = 1L;
		String content = "테스트 시스템 메시지";

		// when
		String messageId = chatMessageService.sendSystemMessage(roomId, content);

		// then
		RoomChatMessage roomChatMessage = chatMessageRepository.findById(messageId).orElse(null);
		assertThat(roomChatMessage).isNotNull();
		assertThat(roomChatMessage.getId()).isEqualTo(messageId);
		assertThat(roomChatMessage.getRoomId()).isEqualTo(roomId);
		assertThat(roomChatMessage.getSenderId()).isEqualTo("SYSTEM");
		assertThat(roomChatMessage.getContent()).isEqualTo(content);
		assertThat(roomChatMessage.getType()).isEqualTo(MessageType.SYSTEM);
	}

	@Test
	@DisplayName("updateMessageDeliveryStatus에서 상태변경 잘 됐는지 DB조회 테스트")
	@Transactional
	void testUpdateMessageDeliveryStatus() {
		// given
		Long roomId = 1L;
		RoomChatMessage message = RoomChatMessage.create(
			roomChatMessageIdGenerator.createId(roomId),
			roomId,
			"user123",
			"테스트 메시지",
			LocalDateTime.now(),
			MessageType.CHAT,
			DeliveryStatus.SENT
		);

		chatMessageRepository.save(message);
		// when
		chatMessageService.updateMessageDeliveryStatus(message.getId(), DeliveryStatus.FAILED);
		// then
		RoomChatMessage updatedMessage = chatMessageRepository.findById(message.getId()).orElse(null);
		assertThat(updatedMessage).isNotNull();
		assertThat(updatedMessage.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
	}
}

