package com.onmoim.server.chat.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ChatRoomMessageId implements Serializable {
	private Long roomId;
	private Long messageSequence;

	public static ChatRoomMessageId create(Long roomId, Long messageSequence) {
		return new ChatRoomMessageId(roomId, messageSequence);
	}

	@Override
	public String toString() {
		return roomId+"-"+messageSequence;
	}
}

