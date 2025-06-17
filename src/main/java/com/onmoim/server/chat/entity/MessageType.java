package com.onmoim.server.chat.entity;

import lombok.Getter;

/**
 * 메시지 타입 열거형
 */
@Getter
public enum MessageType {
	CHAT,       // 일반 채팅 메시지
	JOIN,       // 입장 메시지
	LEAVE,      // 퇴장 메시지
	SYSTEM,      // 시스템 메시지
	SUCCESS,
	ERROR		//예외 및 에러
}
