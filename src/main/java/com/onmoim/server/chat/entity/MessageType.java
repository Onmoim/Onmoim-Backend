package com.onmoim.server.chat.entity;


/**
 * 메시지 타입 열거형
 */
public enum MessageType {
	CHAT,       // 일반 채팅 메시지
	JOIN,       // 입장 메시지
	LEAVE,      // 퇴장 메시지
	SYSTEM,      // 시스템 메시지
	ERROR		//예외 및 에러
}
