package com.onmoim.server.chat.service;

import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.enums.DeliveryStatus;
import com.onmoim.server.chat.service.retry.ChatMessageRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageSendService {
	private final SimpMessagingTemplate messagingTemplate;
	private final ChatMessageRetryService chatMessageRetryService;
	private final ChatStatusService chatStatusService;


	@Transactional
	public void send(String destination, ChatMessageDto message) {
		ChatRoomMessageId messageId = ChatRoomMessageId.create(message.getGroupId(),message.getMessageSequence());

		try {
			// WebSocket을 통해 메시지 전송
			messagingTemplate.convertAndSend(destination, message);

			// 전송 성공 시 SENT 상태 업데이트
			chatStatusService.updateMessageDeliveryStatus(messageId, DeliveryStatus.SENT);
			log.debug("메시지 전송 완료: ID: {}, 방ID: {}", messageId, message.getGroupId());

		} catch (Exception e) {
			// 전송 실패 시 FAILED 상태 업데이트
			chatStatusService.updateMessageDeliveryStatus(messageId, DeliveryStatus.FAILED);
			log.warn("메시지 전송 실패: ID: {}, 방ID: {}, 오류: {}", messageId, message.getGroupId(), e.getMessage());

			// 실패 재시도 처리
			chatMessageRetryService.failedProcess(message, destination);
		}
	}
}
