package com.onmoim.server.chat.service;

import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.dto.ChatRoomResponse;
import com.onmoim.server.chat.entity.ChatRoom;
import com.onmoim.server.chat.entity.ChatRoomMember;
import com.onmoim.server.chat.entity.SubscribeRegistry;
import com.onmoim.server.chat.repository.ChatRoomMemberRepository;
import com.onmoim.server.chat.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomMemberRepository chatRoomMemberRepository;

	/**
	 * 채팅방 생성
	 */
	@Transactional
	public ChatRoomResponse createRoom(String name, String description, String creatorId) {
		// 채팅방 엔티티 생성
		ChatRoom room = ChatRoom.builder()
			.name(name)
			.description(description)
			.creatorId(creatorId)
			.chatRoomMembers(new HashSet<>())
			.build();

		// 채팅방 멤버로 추가
		ChatRoomMember memberEntity = ChatRoomMember.builder()
			.userId(creatorId)
			.build();

		room.addMember(memberEntity);
		chatRoomRepository.save(room);

		log.debug("채팅방이 생성되었습니다. roomId: {}, name: {}, creatorId: {}",
			room.getId(), name, creatorId);

		return ChatRoomResponse.fromChatRoom(room, room.getChatRoomMembers().size(),
			SubscribeRegistry.CHAT_ROOM_SUBSCRIBE_PREFIX.getDestination() + room.getId());
	}
}
