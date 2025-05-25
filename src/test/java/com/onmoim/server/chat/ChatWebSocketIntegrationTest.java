package com.onmoim.server.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onmoim.server.TestSecurityConfig;
import com.onmoim.server.chat.dto.ChatRoomResponse;
import com.onmoim.server.chat.dto.CreateRoomRequest;
import com.onmoim.server.chat.dto.ChatMessageDto;
import com.onmoim.server.chat.entity.MessageType;
import com.onmoim.server.common.response.ResponseHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)  // 새로운 간소화된 보안 설정 적용
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChatWebSocketIntegrationTest {

	@LocalServerPort
	private int port;
	private WebSocketStompClient stompClient;
	private StompSession session;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@BeforeEach
	public void setup() throws ExecutionException, InterruptedException, TimeoutException {
		// 연결 상태를 확인하기 위한 CompletableFuture
		CompletableFuture<Boolean> stompConnected = new CompletableFuture<>();

		// STOMP 세션 핸들러 상세 구현
		StompSessionHandler handler = new StompSessionHandlerAdapter() {
			@Override
			public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
				stompConnected.complete(true);
			}
		};

		stompClient = new WebSocketStompClient(new StandardWebSocketClient());

		// Jackson 메시지 컨버터 설정 (JavaTimeModule 등록)
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		converter.setObjectMapper(objectMapper);

		stompClient.setMessageConverter(converter);

		String wsUrl = "ws://localhost:" + port + "/ws-chat";
		System.out.println("STOMP 연결 시도: " + wsUrl);
		session = stompClient.connect(wsUrl, handler).get(10, TimeUnit.SECONDS);

		// STOMP 연결 확인
		Boolean connected = stompConnected.get(10, TimeUnit.SECONDS);

	}

	@AfterEach
	public void tearDown() {
		if (session != null && session.isConnected()) {
			session.disconnect();
		}
	}

	@Test
	public void testWebSocketConnection() {
		assertTrue(session.isConnected(), "웹소켓 연결이 성공적으로 이루어져야 합니다.");
	}

	@Test
	@DisplayName("채팅방 생성 후 시스템 메시지 수신 테스트")
	public void testSystemMessageAfterRoomCreation() throws Exception {
		// given-1. 메시지 수신을 위한 CompletableFuture 설정
		CompletableFuture<ChatMessageDto> messageFuture = new CompletableFuture<>();

		// given-2. 채팅방 생성 요청 데이터 준비
		CreateRoomRequest createRoomRequest = new CreateRoomRequest();
		createRoomRequest.setName("테스트 채팅방");
		createRoomRequest.setDescription("통합 테스트용 채팅방");

		// given-3. 채팅방 생성 API를 테스트 환경에서 직접 호출
		ChatRoomResponse roomResponse = callPostCreateRoomAPI(createRoomRequest);
		String subscribeDestination = roomResponse.getSubscribeDestination();

		// given-4. API 반환 값을 이용하여 채팅방 구독
		StompSession.Subscription subscription = stompSubscribe(subscribeDestination,
			messageFuture);
		// 구독 설정 대기
		Thread.sleep(1000);

		//when 채팅방 메시지 전송
		ChatMessageDto testMessage = ChatMessageDto.builder()
			.roomId(roomResponse.getId())
			.type(MessageType.SYSTEM)
			.timestamp(LocalDateTime.now())
			.senderName("MANAGER")
			.content("Payload")
			.build();
		messagingTemplate.convertAndSend(subscribeDestination, testMessage);

		//then 메시지 수진
		try {
			// 10. WebSocket으로 시스템 메시지가 수신되는지 확인
			ChatMessageDto receivedMessage = messageFuture.get(3, TimeUnit.SECONDS);
			assertNotNull(receivedMessage, "수신된 메시지가 null이 아니어야 합니다");
		} finally {
			// 구독 해제
			subscription.unsubscribe();
		}
	}

	private StompSession.Subscription stompSubscribe(String subscribeDestination,
		CompletableFuture<ChatMessageDto> messageFuture) {
		StompSession.Subscription subscription = session.subscribe(
			subscribeDestination,
			new StompFrameHandler() {
				@Override
				public Type getPayloadType(StompHeaders headers) {
					return ChatMessageDto.class;
				}

				@Override
				public void handleFrame(StompHeaders headers, Object payload) {
					System.out.println("메시지 수신: " + payload);
					messageFuture.complete((ChatMessageDto)payload);
				}
			}
		);
		return subscription;
	}

	private ChatRoomResponse callPostCreateRoomAPI(CreateRoomRequest createRoomRequest) {
		ResponseEntity<ResponseHandler> response =
			new TestRestTemplate().postForEntity(
				"http://localhost:" + port + "/api/v1/chat/room",
				createRoomRequest,
				ResponseHandler.class
			);

		// 9. 채팅방 생성 응답 확인
		System.out.println("response = " + response);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());

		// Map을 ChatRoomResponse로 변환
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 처리를 위해 필요

		// LinkedHashMap을 ChatRoomResponse로 변환
		return objectMapper.convertValue(response.getBody().getData(), ChatRoomResponse.class);
	}
}