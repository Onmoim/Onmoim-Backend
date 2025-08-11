package com.onmoim.server.chat.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomListUpdateDto {
	/** 채팅방 ID */
	private Long groupId;
	/** 채팅방 정보 */
	private String groupName;
	private long participantCount;

	/** 최신 메시지 */
	private ChatMessageDto latestMessage;
}
