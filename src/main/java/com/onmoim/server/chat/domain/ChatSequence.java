package com.onmoim.server.chat.domain;

import com.onmoim.server.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_sequence")
@Getter
@NoArgsConstructor
public class ChatSequence extends BaseEntity {

	@Id
	private Long roomId;

	@Column(nullable = false)
	private Long currentSequence;

	public ChatSequence(Long roomId, Long currentSequence) {
		this.roomId = roomId;
		this.currentSequence = currentSequence;
	}

	public void setCurrentSequence(Long sequence) {
		this.currentSequence = sequence;
	}
}
