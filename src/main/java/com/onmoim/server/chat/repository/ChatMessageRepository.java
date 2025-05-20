package com.onmoim.server.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.entity.RoomChatMessage;

public interface ChatMessageRepository extends JpaRepository<RoomChatMessage, String> {
}
