package com.onmoim.server.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.domain.ChatRoomMember;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

	List<ChatRoomMember> findByChatRoomId(Long id);

	Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long roomId, Long userId);
}
