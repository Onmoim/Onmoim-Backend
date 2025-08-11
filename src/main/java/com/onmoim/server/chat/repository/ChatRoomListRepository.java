package com.onmoim.server.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.domain.ChatRoom;
import com.onmoim.server.group.repository.GroupUserRepositoryCustom;

public interface ChatRoomListRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {
}
