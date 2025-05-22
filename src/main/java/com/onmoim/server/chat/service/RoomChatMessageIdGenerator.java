package com.onmoim.server.chat.service;

public interface RoomChatMessageIdGenerator {
	Long getSequence(Long roomId);
}

