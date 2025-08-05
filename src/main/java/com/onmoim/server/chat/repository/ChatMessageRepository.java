package com.onmoim.server.chat.repository;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.dto.ChatMessageDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import io.lettuce.core.dynamic.annotation.Param;

public interface ChatMessageRepository extends JpaRepository<ChatRoomMessage, ChatRoomMessageId> {
	@Query("SELECT new com.onmoim.server.chat.domain.dto.ChatMessageDto(" +
		"cm.id.messageSequence, cm.id.roomId, cm.type, cm.content, " +
		"cm.senderId, cm.timestamp, " +
		"u.id, u.name, u.profileImgUrl) " +
		"FROM ChatRoomMessage cm " +
		"JOIN User u ON cm.senderId = u.id " +
		"WHERE cm.id.roomId = :roomId " +
		"AND cm.id.messageSequence < :cursor " +
		"ORDER BY cm.id.messageSequence DESC")
	List<ChatMessageDto> findTop100ByRoomIdWithUserBeforeCursor(
		@Param("roomId") Long roomId,
		@Param("cursor") Long cursor,
		Pageable pageable);

	@Query("SELECT new com.onmoim.server.chat.domain.dto.ChatMessageDto(" +
		"cm.id.messageSequence, cm.id.roomId, cm.type, cm.content, " +
		"cm.senderId, cm.timestamp, " +
		"u.id, u.name, u.profileImgUrl) " +
		"FROM ChatRoomMessage cm " +
		"JOIN User u ON cm.senderId = u.id " +
		"WHERE cm.id.roomId = :roomId " +
		"ORDER BY cm.id.messageSequence DESC")
	List<ChatMessageDto> findTop100ByRoomIdWithUser(
		@Param("roomId") Long roomId,
		Pageable pageable);
}
