package com.onmoim.server.chat.service;

import com.onmoim.server.chat.dto.RoomChatMessageDto;


/**
 * {@link ChatMessageEventHandler}
 */

public record MessageSendEvent(String destination, RoomChatMessageDto message) {
}
