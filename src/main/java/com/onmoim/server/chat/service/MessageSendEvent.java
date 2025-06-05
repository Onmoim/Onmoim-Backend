package com.onmoim.server.chat.service;

import com.onmoim.server.chat.dto.ChatMessageDto;


/**
 * {@link ChatMessageEventHandler}
 */

public record MessageSendEvent(String destination, ChatMessageDto message) {
}
