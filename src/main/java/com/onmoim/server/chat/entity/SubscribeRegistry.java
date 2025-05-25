package com.onmoim.server.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SubscribeRegistry {
	CHAT_ROOM_SUBSCRIBE_PREFIX("/topic/chat.room."),
	ERROR_SUBSCRIBE_DESTINATION("/queue");

	private final String destination;
}
