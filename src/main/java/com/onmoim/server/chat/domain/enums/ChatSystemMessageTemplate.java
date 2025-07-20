package com.onmoim.server.chat.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DB에서 관리하면 template 변경에 용이
 */
@AllArgsConstructor
@Getter
public enum ChatSystemMessageTemplate {
	CREATE_CHAT_ROOM( "채팅방이 생성되었습니다.", "채팅방 생싱시 발송"),
	JOIN_CHAT_ROOM("%s님이 가입했습니다.", "채팅방 가입시 발송"),
	LEAVE_CHAT_ROOM("%s님이 탈퇴했습니다.", "채팅방 나갈때 발송");

	private final String content;
	private final String description;

	public String getContentWithBind(Object... args) {
		return String.format(content, args);
	}
}
