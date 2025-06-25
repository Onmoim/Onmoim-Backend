package com.onmoim.server.chat.dto;

import java.time.LocalDateTime;

import com.onmoim.server.group.entity.Group;
import com.onmoim.server.user.entity.User;

import lombok.Data;

@Data
public class ChatRoomSummaryDto {
	private Long roomId;
	private String roomName;
	private String lastMessage;
	private String lastSenderName;
	private LocalDateTime lastMessageTime;

	private ChatRoomSummaryDto(Long roomId, String roomName, String lastMessage, String lastSenderName,
		LocalDateTime lastMessageTime) {
		this.roomId = roomId;
		this.roomName = roomName;
		this.lastMessage = lastMessage;
		this.lastSenderName = lastSenderName;
		this.lastMessageTime = lastMessageTime;
	}

	public static ChatRoomSummaryDto create(Group group, User user, ChatMessageDto message) {
		return new ChatRoomSummaryDto(
			group.getId(),
			group.getName(),
			message.getContent(),
			user.getName(),
			message.getTimestamp()
		) ;
	}
}
