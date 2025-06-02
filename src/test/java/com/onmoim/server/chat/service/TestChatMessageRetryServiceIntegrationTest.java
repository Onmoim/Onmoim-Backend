// package com.onmoim.server.chat.service;
//
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;
//
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.test.annotation.DirtiesContext;
// import org.springframework.transaction.annotation.Transactional;
//
// import com.onmoim.server.chat.dto.ChatMessageDto;
// import com.onmoim.server.chat.entity.ChatRoomMessageId;
// import com.onmoim.server.chat.entity.DeliveryStatus;
//
// @SpringBootTest
// @Transactional
// public class TestChatMessageRetryServiceIntegrationTest {
//
// 	@Autowired
// 	private ChatMessageRetryService chatMessageRetryService;
//
// 	@MockBean
// 	private SimpMessagingTemplate messagingTemplate;
//
// 	@MockBean
// 	private ChatMessageService chatMessageService;
//
// 	@AfterEach
// 	public void tearDown() {
// 		// 테스트 후 정리 작업
// 		reset(messagingTemplate, chatMessageService);
// 	}
//
//
// 	@Test
// 	@DisplayName("ChatMessageRetryService 재시도 3번 되는지 ")
// 	void testRetryAndRecover() throws Exception {
// 		// 테스트 데이터 설정
// 		ChatMessageDto testMessage = new ChatMessageDto();
// 		long roomId = 1L;
// 		ChatRoomMessageId messageId = ChatRoomMessageId.create(roomId, 1L);
// 		testMessage.setMessageSequence(messageId.getMessageSequence());
// 		testMessage.setRoomId(messageId.getRoomId());
// 		testMessage.setRoomId(roomId);
// 		String testDestination = "/topic/chat.room.1";
//
// 		// 항상 예외를 발생시키도록 설정
// 		doThrow(new RuntimeException("테스트 예외"))
// 			.when(messagingTemplate).convertAndSend(eq(testDestination), any(ChatMessageDto.class));
//
// 		try {
// 			// 메소드 호출
// 			chatMessageRetryService.failedProcess(testMessage, testDestination);
// 		} catch (Exception e) {
// 			// 예외 예상
// 		}
//
// 		// 충분한 시간 대기 (재시도 시간 고려)
// 		Thread.sleep(5000);
//
// 		// messagingTemplate.convertAndSend가 3번 호출되었는지 확인
// 		verify(messagingTemplate, times(3)).convertAndSend(eq(testDestination), any(ChatMessageDto.class));
//
// 		// 최종적으로 FAILED_PERMANENTLY 상태로 업데이트되었는지 확인
// 		verify(chatMessageService).updateMessageDeliveryStatus(
// 			eq(messageId),
// 			eq(DeliveryStatus.FAILED_PERMANENTLY));
// 	}
//
// 	@Test
// 	@DisplayName("ChatMessageRetryService 재시도 3번 후 최종 실패 로직 동작하는지 ")
// 	void shouldRetryThreeTimesAndUpdateToFailedPermanently() throws Exception {
// 		// 테스트 데이터 준비
// 		ChatMessageDto testMessage = new ChatMessageDto();
// 		long roomId = 1L;
// 		ChatRoomMessageId messageId = ChatRoomMessageId.create(roomId, 1L);
// 		testMessage.setMessageSequence(messageId.getMessageSequence());
// 		testMessage.setRoomId(messageId.getRoomId());
// 		testMessage.setRoomId(roomId);
// 		String testDestination = "/topic/chat.room.1";
//
// 		// messagingTemplate가 항상 예외를 발생시키도록 설정
// 		doThrow(new RuntimeException("연결 오류 발생"))
// 			.when(messagingTemplate).convertAndSend(anyString(), any(ChatMessageDto.class));
//
// 		try {
// 			// 메시지 전송 시도 (이 호출은 내부적으로 재시도 메커니즘을 트리거함)
// 			chatMessageRetryService.failedProcess(testMessage, testDestination);
// 		} catch (Exception e) {
// 			// 예외는 무시 (최종적으로 예외가 발생할 것으로 예상됨)
// 		}
//
// 		// 재시도 과정이 완료될 때까지 충분한 시간 대기
// 		// 재시도 간격은 @Backoff 설정에 따라 500ms, 1000ms, 2000ms...
// 		Thread.sleep(5000);
//
// 		// 메시지 전송이 정확히 3번 시도되었는지 확인
// 		verify(messagingTemplate, times(3)).convertAndSend(eq(testDestination), any(ChatMessageDto.class));
//
// 		// recoverFailedMessage 메소드의 효과로 FAILED_PERMANENTLY 상태로 업데이트되었는지 확인
// 		verify(chatMessageService).updateMessageDeliveryStatus(
// 			eq(messageId),
// 			eq(DeliveryStatus.FAILED_PERMANENTLY));
// 	}
// }