package com.onmoim.server.chat.service;

import org.springframework.stereotype.Service;

import com.onmoim.server.chat.domain.ChatRoomMember;
import com.onmoim.server.chat.repository.ChatRoomMemberRepository;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomMemberQueryService {

	private final ChatRoomMemberRepository chatRoomMemberRepository;

	public ChatRoomMember getByChatRoomIdAndUserId(Long roomId, Long userId) {
		return chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId,userId)
			.orElseThrow(() -> new CustomException(ErrorCode.IS_NOT_CHAT_ROOM_MEMBER));
	}
}
