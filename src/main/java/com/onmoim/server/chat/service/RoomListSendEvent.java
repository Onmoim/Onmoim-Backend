package com.onmoim.server.chat.service;

import com.onmoim.server.chat.dto.ChatRoomListUpdateDto;

/**
 * {@link ChatMessageEventHandler}
 */

public record RoomListSendEvent(String destination, ChatRoomListUpdateDto chatRoomListUpdateDto) {
}
