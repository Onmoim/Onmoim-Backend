package com.onmoim.server.chat.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SubscribeRegistry {
	CHAT_ROOM_SUBSCRIBE_PREFIX("/topic/chat.room."),
	SYSTEM_MESSAGE_PREFIX("/queue");

	private final String destination;
}
