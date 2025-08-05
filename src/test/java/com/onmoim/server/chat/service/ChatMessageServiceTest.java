package com.onmoim.server.chat.service;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.dto.ChatUserDto;
import com.onmoim.server.chat.domain.enums.DeliveryStatus;
import com.onmoim.server.chat.domain.enums.MessageType;
import com.onmoim.server.chat.repository.ChatMessageRepository;
import com.onmoim.server.chat.repository.RoomChatMessageIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
		List<ChatMessageDto> mockMessages = List.of(
			createMessage(roomId, 101L),
			createMessage(roomId, 100L)
		);

		Pageable pageable = PageRequest.of(0, 100);

		when(chatMessageRepository.findTop100ByRoomIdWithUser(roomId, pageable))
			.thenReturn(mockMessages);

		List<ChatMessageDto> result = chatMessageService.getMessages(roomId, null);

		verify(chatMessageRepository, times(1))
			.findTop100ByRoomIdWithUser(roomId, pageable);
		verify(chatMessageRepository, never())
			.findTop100ByRoomIdWithUserBeforeCursor(anyLong(), anyLong(), any());

		assertThat(result).isEqualTo(mockMessages);
	}

	@Test
	void getMessages_커서있음_이전100개조회() {
		Long roomId = 1L;
		Long cursor = 50L;
		List<ChatMessageDto> mockMessages = List.of(
			createMessage(roomId, 49L),
			createMessage(roomId, 48L)
		);
		Pageable pageable = PageRequest.of(0, 100);

		when(chatMessageRepository.findTop100ByRoomIdWithUserBeforeCursor(roomId, cursor, pageable))
			.thenReturn(mockMessages);

		List<ChatMessageDto> result = chatMessageService.getMessages(roomId, cursor);

		verify(chatMessageRepository, never())
			.findTop100ByRoomIdWithUser(anyLong(), any());
		verify(chatMessageRepository, times(1))
			.findTop100ByRoomIdWithUserBeforeCursor(roomId, cursor, pageable);

		assertThat(result).isEqualTo(mockMessages);
	}

	// 편의 메서드: 테스트용 메시지 생성
	private ChatMessageDto createMessage(Long roomId, Long messageSequence) {
		ChatRoomMessage chatRoomMessage = ChatRoomMessage.create(
			ChatRoomMessageId.create(roomId, messageSequence),
			123L,
			"test content",
			LocalDateTime.now(),
			MessageType.CHAT,
			DeliveryStatus.PENDING
		);
		return ChatMessageDto.of(
			chatRoomMessage,
			ChatUserDto.builder()
				.profileImageUrl("profileImage")
				.isOwner(true)
				.username("name")
				.id(1L)
				.build()
		);
	}
}

