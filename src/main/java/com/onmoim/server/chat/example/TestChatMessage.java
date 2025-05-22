package com.onmoim.server.chat.example;

import java.time.LocalDateTime;

import com.onmoim.server.chat.entity.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestChatMessage {

	private String messageId;
	private String senderId;
	private String content;
	private LocalDateTime timestamp;
	private MessageType type;

}
