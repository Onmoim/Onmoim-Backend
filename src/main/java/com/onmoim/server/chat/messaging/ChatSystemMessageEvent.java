package com.onmoim.server.chat.messaging;

/**
 * {@link com.onmoim.server.chat.messaging.ChatMessageEventHandler}
 */
public record ChatSystemMessageEvent(Long groupId, String content) {
}
