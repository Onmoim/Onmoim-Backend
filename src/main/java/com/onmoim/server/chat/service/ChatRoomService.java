package com.onmoim.server.chat.service;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.enums.DeliveryStatus;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.domain.dto.ChatRoomResponse;
import com.onmoim.server.chat.domain.ChatRoom;
import com.onmoim.server.chat.domain.enums.SubscribeRegistry;
import com.onmoim.server.chat.repository.ChatRoomRepository;
import com.onmoim.server.group.implement.GroupUserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final GroupUserQueryService groupUserQueryService;

	/**
	 * 채팅방 생성
	 */
	@Transactional
	public ChatRoomResponse createRoom(Long groupId, String name, String description, Long creatorId) {
		// 채팅방 엔티티 생성
		ChatRoom room = ChatRoom.builder()
			.name(name)
			.description(description)
			.groupId(groupId)
			.creatorId(creatorId)
			.build();

		Long groupMemberCount = groupUserQueryService.countMembers(groupId);
		chatRoomRepository.save(room);

		log.debug("채팅방이 생성되었습니다. roomId: {}, name: {}, creatorId: {}",
			groupId, name, creatorId);

		return ChatRoomResponse.fromChatRoom(room, groupMemberCount,
			SubscribeRegistry.CHAT_ROOM_SUBSCRIBE_PREFIX.getDestination() + groupId);
	}
}
