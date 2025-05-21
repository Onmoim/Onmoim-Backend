package com.onmoim.server.chat.facade;

import org.springframework.stereotype.Service;

import com.onmoim.server.chat.dto.ChatRoomResponse;
import com.onmoim.server.chat.dto.CreateRoomRequest;
import com.onmoim.server.chat.service.ChatMessageService;
import com.onmoim.server.chat.service.ChatRoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomFacade {

	private final ChatRoomService chatRoomService;
	private final ChatMessageService chatMessageService;

	/**
	 * 채팅방 생성 이후 ChatRoomResponse.subscribeDestination 응답을 통해 Client가 구독합니다.
	 * 이 때문에 "채팅방이 생성되었습니다." 메시지는 즉시 전달되지는 않지만, "채팅 이력 조회(DB)"를 통해 [채팅방 목록 || 채팅방 진입]시 조회됩니다.
	 */
	public ChatRoomResponse createRoom(CreateRoomRequest request, Long userId) {

		ChatRoomResponse room = chatRoomService.createRoom(request.getName(), request.getDescription(), userId);

		chatMessageService.sendSystemMessage(room.getId(), "채팅방이 생성되었습니다.");

		return room;
	}
}
