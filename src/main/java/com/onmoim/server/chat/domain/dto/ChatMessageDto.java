package com.onmoim.server.chat.domain.dto;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.enums.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 응답 객체
 */
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class ChatMessageDto {

    @Schema(description = "메시지 고유 ID", example = "9876")
    private Long messageSequence;

    @Schema(description = "채팅방 ID", example = "12345")
    private Long groupId;

    @Schema(description = "메시지 타입", example = "TEXT")
    private MessageType type;

    @Schema(description = "메시지 내용", example = "이번 주 모임은 화요일입니다.")
    private String content;

    @Schema(description = "발신자 ID (서버에서 Principal 기반으로 설정)", example = "54321")
    private Long senderId;

    @Schema(description = "발신자 이름 및 정보 (UI 표시용)")
    private ChatUserDto chatUserDto;

    @Schema(description = "발송 시간", example = "2025-08-11T14:00:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ChatMessageDto of(ChatRoomMessage entity, ChatUserDto chatUserDto) {
        return ChatMessageDto.builder()
            .messageSequence(entity.getId().getMessageSequence())
            .groupId(entity.getId().getRoomId())
            .type(entity.getType())
            .content(entity.getContent())
            .senderId(entity.getSenderId())
            .timestamp(entity.getTimestamp())
            .chatUserDto(chatUserDto)
            .build();
    }

    // 프로젝션용 생성자 추가
    public ChatMessageDto(Long messageSequence, Long groupId, MessageType type,
        String content, Long senderId, LocalDateTime timestamp,
        Long userId, String userName, String profileImgUrl) {
        this.messageSequence = messageSequence;
        this.groupId = groupId;
        this.type = type;
        this.content = content;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.chatUserDto = ChatUserDto.builder()
            .id(userId)
            .username(userName)
            .profileImageUrl(profileImgUrl)
            .build();
    }
}
