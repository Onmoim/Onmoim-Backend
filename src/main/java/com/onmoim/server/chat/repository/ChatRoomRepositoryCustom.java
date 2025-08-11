package com.onmoim.server.chat.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.onmoim.server.chat.domain.dto.ChatRoomSummeryDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;

public interface ChatRoomRepositoryCustom {
	List<ChatRoomSummeryDto> findJoinedGroupList(Long userId, LocalDateTime cursorTime, String cursorGroupName, int size);
}
