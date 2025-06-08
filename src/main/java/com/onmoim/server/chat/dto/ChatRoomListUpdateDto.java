package com.onmoim.server.chat.dto;

import java.util.List;

import lombok.Data;

@Data
public class ChatRoomListUpdateDto {
	private String type = "CHAT_LIST_UPDATE";
	private List<ChatRoomSummaryDto> rooms;

	public ChatRoomListUpdateDto(List<ChatRoomSummaryDto> rooms) {
		this.rooms = rooms;
	}
}