package com.onmoim.server.chat.repository;

public interface RoomChatMessageIdGenerator {
	Long getSequence(Long roomId);
}

