package com.onmoim.server.chat.common.example;

import static com.onmoim.server.chat.domain.enums.SubscribeRegistry.*;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SocketTestController {

	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * Client에서 테스트 하는 방법
	 * Test web site (https://jiangxy.github.io/websocket-debug-tool/)
	 *
	 * 1. [연결] ws://localhost:{port}/ws-chat
	 * 2. [구독] /system/queue
	 * 3. [전송] /app/example.error
	 * //BODY
	 * {
	 * 	"messageId": "msg-001",
	 * 	"senderId": "user123",
	 * 	"content": "error", // error 문자열 포함시 Excpetion 발생
	 * 	"timestamp": "2025-05-16T17:00:00",
	 * 	"type": "CHAT"
	 *    }
	 */

	/**
	 * @RESEPONSE {
	 * "messageId":"fb92e9ba-bd2d-4e08-ac41-834b2d59cd59",
	 * "senderId":"SYSTEM","content":"메시지 처리 중 오류가 발생했습니다: 테스트용 에러입니다.",
	 * "timestamp":"2025-05-16T18:07:02.148384",
	 * "type":"ERROR"
	 * }
	 */

	@MessageMapping("/example.error")
	public void handleChatMessage(TestChatMessage message, Principal principal) {
		if (message.getContent().contains("error")) {
			throw new RuntimeException("테스트용 에러입니다.");
		}

		String destination = ERROR_SUBSCRIBE_DESTINATION.getDestination();
		messagingTemplate.convertAndSendToUser(principal.getName(), destination, "정상 처리되었습니다.");
	}

}
