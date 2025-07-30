package com.onmoim.server.chat.service.retry;

import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.enums.DeliveryStatus;
import com.onmoim.server.chat.service.ChatStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageRetryService {
	private final SimpMessagingTemplate messagingTemplate;
	private final ChatStatusService chatStatusService;

	@Retryable(
		retryFor = Exception.class,
		maxAttempts = 3,
		backoff = @Backoff(delay = 100, multiplier = 1.5, maxDelay = 1000, random = true)
	) // 약 100ms -> 150ms -> 225ms
	public void failedProcess(ChatMessageDto message, String destination) {
		ChatRoomMessageId messageId = ChatRoomMessageId.create(message.getGroupId(), message.getMessageSequence());
		log.debug("메시지 재전송 시도: ID: {}, 방ID: {}", messageId, message.getGroupId());

		// WebSocket을 통해 메시지 재전송
		messagingTemplate.convertAndSend(destination, message);

		// 전송 성공 시 상태 업데이트
		chatStatusService.updateMessageDeliveryStatus(messageId, DeliveryStatus.SENT);

		log.debug("메시지 재전송 성공: ID: {}, 방ID: {}", messageId, message.getGroupId());
	}

	@Recover
	public void recoverFailedMessage(Exception e, ChatMessageDto message, String destination) {
		ChatRoomMessageId messageId = ChatRoomMessageId.create(message.getGroupId(), message.getMessageSequence());

		log.warn("메시지 재전송 최종 실패: ID: {}, 방ID: {}, 최대 시도 횟수 초과(3회), 오류: {}",
			messageId, message.getGroupId(), e.getMessage());

		chatStatusService.updateMessageDeliveryStatus(messageId, DeliveryStatus.FAILED_PERMANENTLY);
	}
}
