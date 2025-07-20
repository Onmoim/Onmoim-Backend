package com.onmoim.server.chat.service;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.enums.DeliveryStatus;
import com.onmoim.server.chat.domain.enums.MessageType;
import com.onmoim.server.chat.repository.ChatMessageRepository;
import com.onmoim.server.chat.repository.RoomChatMessageIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatMessageServiceTest {

	@Mock
	ChatMessageRepository chatMessageRepository;

	@Mock
	RoomChatMessageIdGenerator roomChatMessageIdGenerator;

	@Mock
	ChatMessageSendService chatMessageSendService;

	@InjectMocks
	ChatMessageService chatMessageService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void getMessages_커서없음_최신100개조회() {
		Long roomId = 1L;
		List<ChatRoomMessage> mockMessages = List.of(
			createMessage(roomId, 101L),
			createMessage(roomId, 100L)
		);

		when(chatMessageRepository.findTop100ByIdRoomIdOrderByIdMessageSequenceDesc(roomId))
			.thenReturn(mockMessages);

		List<ChatRoomMessage> result = chatMessageService.getMessages(roomId, null);

		verify(chatMessageRepository, times(1))
			.findTop100ByIdRoomIdOrderByIdMessageSequenceDesc(roomId);
		verify(chatMessageRepository, never())
			.findTop100ByIdRoomIdAndIdMessageSequenceLessThanOrderByIdMessageSequenceDesc(anyLong(), anyLong());

		assertThat(result).isEqualTo(mockMessages);
	}

	@Test
	void getMessages_커서있음_이전100개조회() {
		Long roomId = 1L;
		Long cursor = 50L;
		List<ChatRoomMessage> mockMessages = List.of(
			createMessage(roomId, 49L),
			createMessage(roomId, 48L)
		);

		when(chatMessageRepository.findTop100ByIdRoomIdAndIdMessageSequenceLessThanOrderByIdMessageSequenceDesc(roomId, cursor))
			.thenReturn(mockMessages);

		List<ChatRoomMessage> result = chatMessageService.getMessages(roomId, cursor);

		verify(chatMessageRepository, never())
			.findTop100ByIdRoomIdOrderByIdMessageSequenceDesc(anyLong());
		verify(chatMessageRepository, times(1))
			.findTop100ByIdRoomIdAndIdMessageSequenceLessThanOrderByIdMessageSequenceDesc(roomId, cursor);

		assertThat(result).isEqualTo(mockMessages);
	}

	// 편의 메서드: 테스트용 메시지 생성
	private ChatRoomMessage createMessage(Long roomId, Long messageSequence) {
		return ChatRoomMessage.create(
			ChatRoomMessageId.create(roomId, messageSequence),
			123L,
			"test content",
			LocalDateTime.now(),
			MessageType.CHAT,
			DeliveryStatus.PENDING
		);
	}
}

