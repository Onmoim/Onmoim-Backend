package com.onmoim.server.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.entity.ChatRoomMember;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

	List<ChatRoomMember> findByChatRoomId(Long id);
}
