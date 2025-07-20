package com.onmoim.server.chat.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChatSystemMessageTemplate {
	CREATE_CHAT_ROOM( "채팅방이 생성되었습니다.", "채팅방 생싱서 발송");
	private final String content;
	private final String description;
}
