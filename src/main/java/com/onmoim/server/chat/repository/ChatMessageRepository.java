package com.onmoim.server.chat.repository;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.ChatRoomMessageId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatRoomMessage, ChatRoomMessageId> {
	List<ChatRoomMessage> findTop100ByIdRoomIdAndIdMessageSequenceLessThanOrderByIdMessageSequenceDesc(Long roomId, Long cursor);
	List<ChatRoomMessage> findTop100ByIdRoomIdOrderByIdMessageSequenceDesc(Long roomId);
}
