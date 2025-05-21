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
import com.onmoim.server.chat.entity.ChatRoomMessage;
import com.onmoim.server.chat.entity.ChatRoomMessageId;
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
		ChatRoomMessageId messageId = chatMessageService.sendSystemMessage(roomId, content);

		// then
		ChatRoomMessage chatRoomMessage = chatMessageRepository.findById(messageId).orElse(null);
		assertThat(chatRoomMessage).isNotNull();
		assertThat(chatRoomMessage.getId()).isEqualTo(messageId);
		assertThat(chatRoomMessage.getSenderId()).isEqualTo("SYSTEM");
		assertThat(chatRoomMessage.getContent()).isEqualTo(content);
		assertThat(chatRoomMessage.getType()).isEqualTo(MessageType.SYSTEM);
	}

	@Test
	@DisplayName("updateMessageDeliveryStatus에서 상태변경 잘 됐는지 DB조회 테스트")
	@Transactional
	void testUpdateMessageDeliveryStatus() {
		// given
		Long roomId = 1L;
		ChatRoomMessage message = ChatRoomMessage.create(
			ChatRoomMessageId.create(roomId, roomChatMessageIdGenerator.getSequence(roomId)),
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
		ChatRoomMessage updatedMessage = chatMessageRepository.findById(message.getId()).orElse(null);
		assertThat(updatedMessage).isNotNull();
		assertThat(updatedMessage.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
	}
}

