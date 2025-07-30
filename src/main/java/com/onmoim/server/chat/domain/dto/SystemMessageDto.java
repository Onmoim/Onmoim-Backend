package com.onmoim.server.chat.domain.dto;

import java.time.LocalDateTime;

import com.onmoim.server.chat.domain.enums.MessageType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class SystemMessageDto {
	private MessageType type;

	/** 메시지 내용 */
	private String content;
	@Builder.Default
	private LocalDateTime timestamp = LocalDateTime.now();
}
