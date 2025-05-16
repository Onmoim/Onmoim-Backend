package com.onmoim.server.chat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

	private String messageId;
	private String senderId;
	private String content;
	private LocalDateTime timestamp;
	private MessageType type;

	public enum MessageType {
		CHAT, JOIN, LEAVE, ERROR
	}
}
