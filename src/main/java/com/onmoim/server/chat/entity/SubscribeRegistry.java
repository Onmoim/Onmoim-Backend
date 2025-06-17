package com.onmoim.server.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SubscribeRegistry {
	CHAT_ROOM_SUBSCRIBE_PREFIX("/topic/chat.room."),
	CHAT_ROOM_LIST_SUBSCRIBE_PREFIX("/topic/chat.list."),
	ERROR_SUBSCRIBE_DESTINATION("/queue");

	private final String destination;
}
