package com.onmoim.server.chat.service;

public interface RoomChatMessageIdGenerator {
	String createId(Long roomId);
}
