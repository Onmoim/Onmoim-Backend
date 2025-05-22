package com.onmoim.server.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

}
