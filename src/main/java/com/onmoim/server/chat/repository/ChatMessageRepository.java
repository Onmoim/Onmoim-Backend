package com.onmoim.server.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.entity.ChatRoomMessage;
import com.onmoim.server.chat.entity.ChatRoomMessageId;

public interface ChatMessageRepository extends JpaRepository<ChatRoomMessage, ChatRoomMessageId> {
}
