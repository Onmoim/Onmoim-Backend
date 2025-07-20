package com.onmoim.server.chat.domain.dto;

import com.onmoim.server.chat.domain.ChatRoom;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅방 응답 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {

	@Schema(description = "모임ID")
	private Long groupId;

	@Schema(description = "모임 이름")
	private String name;
	@Schema(description = "모임 설명")
	private String description;
	@Schema(description = "카테고리 ID")
	private Long creatorId;
	@Schema(description = "모임 인원 수")
	private Long memberCount;
	@Schema(description = "채팅방 구독 destination")
	private String subscribeDestination;

	public static ChatRoomResponse fromChatRoom(ChatRoom chatRoom, Long memberCount, String subscribeDestination) {
		return ChatRoomResponse.builder()
			.groupId(chatRoom.getId())
			.name(chatRoom.getName())
			.description(chatRoom.getDescription())
			.creatorId(chatRoom.getCreatorId())
			.memberCount(memberCount)
			.subscribeDestination(subscribeDestination)
			.build();
	}
}
