package com.onmoim.server.chat.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMessageId implements Serializable {
	private Long roomId;
	private Long messageSequence;

	private ChatRoomMessageId(Long roomId, Long messageSequence) {
		this.roomId = roomId;
		this.messageSequence = messageSequence;
	}

	public static ChatRoomMessageId create(Long roomId, Long messageSequence) {
		if (roomId == null || messageSequence == null) {
			throw new IllegalArgumentException("roomId와 messageSequence는 null일 수 없습니다");
		}
		return new ChatRoomMessageId(roomId, messageSequence);
	}

	@Override
	public String toString() {
		return roomId+"-"+messageSequence;
	}
}

