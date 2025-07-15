package com.onmoim.server.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.ChatRoomMessageId;

public interface ChatMessageRepository extends JpaRepository<ChatRoomMessage, ChatRoomMessageId> {
}
