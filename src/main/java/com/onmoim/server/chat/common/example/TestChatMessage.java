package com.onmoim.server.chat.common.example;

import com.onmoim.server.chat.domain.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
