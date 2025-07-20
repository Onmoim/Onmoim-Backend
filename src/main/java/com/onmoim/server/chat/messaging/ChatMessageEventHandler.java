package com.onmoim.server.chat.messaging;

import com.onmoim.server.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventHandler {
	private final ChatMessageService chatMessageService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void systemChatListener(ChatSystemMessageEvent event) {
		chatMessageService.sendSystemMessage(event.groupId(), event.content());
	}
}
