package com.onmoim.server.chat.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.onmoim.server.chat.dto.ChatMessageDto;
import com.onmoim.server.chat.entity.ChatRoomMessageId;
import com.onmoim.server.chat.entity.DeliveryStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 이 곳에서 예외메시지를 제외한 모든 메시지 발송을 책임집니다.
 * DB 메시지 저장 커밋 후에 진행되도록 구성하기 위해 SpringEvent를 이용했습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventHandler {
	private final SimpMessagingTemplate messagingTemplate;
	private final ChatMessageService chatMessageService;
	private final ChatMessageRetryService chatMessageRetryService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleMessageSend(MessageSendEvent event) {
		ChatMessageDto message = event.message();
		ChatRoomMessageId messageId = message.getMessageId();
		String destination = event.destination();

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
