package com.onmoim.server.chat.entity;

import java.time.LocalDateTime;

import com.onmoim.server.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 엔티티 클래스
 */
@Entity
@Table(name = "chat_room_messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMessage extends BaseEntity {

	@EmbeddedId
	private ChatRoomMessageId id;

	@Column(name = "sender_id")
	private String senderId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false)
	private LocalDateTime timestamp;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private MessageType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_status")
	private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING; // 기본값은 PENDING

	public static ChatRoomMessage create(
		ChatRoomMessageId id,
		String senderId,
		String content,
		LocalDateTime timestamp,
		MessageType type,
		DeliveryStatus deliveryStatus) {
		return new ChatRoomMessage(id, senderId, content, timestamp, type, deliveryStatus);
	}

	public void setDeliveryStatus(DeliveryStatus status) {
		this.deliveryStatus = status;
	}

}
