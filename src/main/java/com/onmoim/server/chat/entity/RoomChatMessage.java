package com.onmoim.server.chat.entity;

import java.time.LocalDateTime;

import com.onmoim.server.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 엔티티 클래스
 */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor
public class RoomChatMessage extends BaseEntity {

	@Id
	private String id;

	@Column(name = "room_id")
	private Long roomId;

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

	private RoomChatMessage(String id, Long roomId, String senderId, String content, LocalDateTime timestamp,
		MessageType type, DeliveryStatus deliveryStatus) {
		this.id = id;
		this.roomId = roomId;
		this.senderId = senderId;
		this.content = content;
		this.timestamp = timestamp;
		this.type = type;
		this.deliveryStatus = deliveryStatus;
	}

	public static RoomChatMessage create(
		String id,
		Long roomId,
		String senderId,
		String content,
		LocalDateTime timestamp,
		MessageType type,
		DeliveryStatus deliveryStatus) {
		return new RoomChatMessage(id, roomId, senderId, content, timestamp, type, deliveryStatus);
	}

	public void setDeliveryStatus(DeliveryStatus status) {
		this.deliveryStatus = status;
	}
}
