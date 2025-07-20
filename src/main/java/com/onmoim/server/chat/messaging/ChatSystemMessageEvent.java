package com.onmoim.server.chat.messaging;

import com.onmoim.server.chat.domain.enums.ChatSystemMessageTemplate;

/**
 * {@link com.onmoim.server.chat.messaging.ChatMessageEventHandler}
 */
public record ChatSystemMessageEvent(Long groupId, String content) {
}
