package com.onmoim.server.chat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.domain.dto.ChatRoomSummeryDto;
import com.onmoim.server.chat.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomListService {

	private final ChatRoomRepository chatRoomRepository;

	public List<ChatRoomSummeryDto> getChatRoomList(Long userId, LocalDateTime cursorTime, String cursorGroupName, int size) {
		return chatRoomRepository.findJoinedGroupList(userId, cursorTime, cursorGroupName, size);
	}
}
