package com.onmoim.server.chat.service;

import static com.onmoim.server.chat.entity.SubscribeRegistry.*;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.dto.ChatMessageDto;
import com.onmoim.server.chat.dto.ChatRoomListUpdateDto;
import com.onmoim.server.chat.dto.ChatRoomSummaryDto;
import com.onmoim.server.group.dto.response.GroupMembersResponseDto;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.service.GroupQueryService;
import com.onmoim.server.group.service.GroupUserQueryService;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomListService {

	private final GroupUserQueryService groupUserQueryService;
	private final GroupQueryService groupQueryService;
	private final UserQueryService userQueryService;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public void chatListUpdate(Long groupId, ChatMessageDto chatMessageDto){

		//일단 모든 채팅방 멤버를 가져오도록 구성 TODO, 리팩토링 대상
		int size = Integer.parseInt(groupUserQueryService.countGroupMembers(groupId).toString());
		var groupUserAndMembers = groupUserQueryService
			.findGroupUserAndMembers(groupId, null, size);

		Group group = groupQueryService.getById(groupId);

		Long senderId = chatMessageDto.getSenderId();

		User user = userQueryService.findById(senderId);

		/**
		 *
		 * {"type":"CHAT_LIST_UPDATE","rooms":[{"roomId":4,"roomName":"소규모 모임","lastMessage":"안녕하세요! 오늘 날씨가 정말 좋네요.","lastSenderName":"홍석준","lastMessageTime":[2025,6,12,22,53,48,414507000]}]}
		 */
		for (GroupMembersResponseDto dto : groupUserAndMembers.getContent()) {
			String destination = CHAT_ROOM_LIST_SUBSCRIBE_PREFIX.getDestination() + dto.getUserId();
			ChatRoomSummaryDto chatRoomSummaryDto = ChatRoomSummaryDto.create(group, user, chatMessageDto);
			eventPublisher.publishEvent(
				new RoomListSendEvent(
					destination,
					new ChatRoomListUpdateDto(List.of(chatRoomSummaryDto))
				)
			);
		}
	}
}
