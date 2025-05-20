package com.onmoim.server.chat.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.dto.RoomChatMessageDto;
import com.onmoim.server.chat.entity.DeliveryStatus;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TestChatMessageEventHandlerIntegrationTest {

	@Autowired
	private ChatMessageEventHandler chatMessageEventHandler;

	@MockBean
	private SimpMessagingTemplate messagingTemplate;

	@MockBean
	private ChatMessageService chatMessageService;


	private RoomChatMessageDto testMessage;
	private String testDestination;
	private String testMessageId;

	@AfterEach
	void tearDown() {
		// 테스트 후 정리 작업
		reset(messagingTemplate, chatMessageService);
	}

	@BeforeEach
	void setUp() {
		testMessageId = UUID.randomUUID().toString();
		testMessage = RoomChatMessageDto.builder()
			.messageId(testMessageId)
			.roomId(1L)
			.content("테스트 메시지")
			.senderId("user-123")
			.build();
		testDestination = "/topic/chat/room-123";
	}

	@Test
	@DisplayName("MessageEventHandler 전송 완료후 메시지 상태가 SENT로 변경되는지")
	void testSuccessfulMessageDelivery() {
		// 메시지 이벤트 생성
		MessageSendEvent event = new MessageSendEvent(testDestination, testMessage);

		// 메시지 전송 성공 시나리오
		doNothing().when(messagingTemplate).convertAndSend(anyString(), any(RoomChatMessageDto.class));
		doNothing().when(chatMessageService).updateMessageDeliveryStatus(anyString(), any(DeliveryStatus.class));

		// 이벤트 핸들러 호출
		chatMessageEventHandler.handleMessageSend(event);

		// 메시지가 한 번 전송되었는지 확인
		verify(messagingTemplate, times(1)).convertAndSend(testDestination, testMessage);
		// 상태가 SENT로 업데이트 되었는지 확인
		verify(chatMessageService, times(1)).updateMessageDeliveryStatus(testMessageId, DeliveryStatus.SENT);
	}

	@Test
	@DisplayName("MessageEventHandler Retry로 메시지 상태가 SENT로 변경되는지")
	void testMessageDeliveryRetrySuccessful() {
		// 메시지 이벤트 생성
		MessageSendEvent event = new MessageSendEvent(testDestination, testMessage);

		// 첫 번째 전송 시도에서 예외 발생
		doThrow(new RuntimeException("전송 실패 시뮬레이션"))
			.doNothing() // 두 번째 시도에서는 성공
			.when(messagingTemplate).convertAndSend(anyString(), any(RoomChatMessageDto.class));

		// 이벤트 핸들러 호출
		chatMessageEventHandler.handleMessageSend(event);

		// 메시지 전송이 총 2번 시도되었는지 확인 (최초 1번 + 재시도 1번)
		verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(RoomChatMessageDto.class));

		// 상태가 FAILED로 업데이트된 후 SENT로 업데이트되었는지 확인
		verify(chatMessageService, times(1)).updateMessageDeliveryStatus(testMessageId, DeliveryStatus.FAILED);
		verify(chatMessageService, times(1)).updateMessageDeliveryStatus(testMessageId, DeliveryStatus.SENT);
	}
}
