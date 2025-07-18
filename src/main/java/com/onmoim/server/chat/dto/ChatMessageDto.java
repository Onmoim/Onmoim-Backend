package com.onmoim.server.chat.dto;

import java.time.LocalDateTime;

import com.onmoim.server.chat.entity.ChatRoomMessage;
import com.onmoim.server.chat.entity.MessageType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 응답 객체
 */
@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class ChatMessageDto {
    
    /** 메시지 고유 ID */
    private Long messageSequence;
    
    /** 채팅방 ID */
    private Long roomId;

    private Long groupId;
    
    /** 메시지 타입 */
    private MessageType type;
    
    /** 메시지 내용 */
    private String content;
    
    /** 발신자 ID (서버에서 Principal 기반으로 설정) */
    private Long senderId;
    
    /** 발신자 이름 (UI 표시용) */
    private ChatUserDto chatUserDto;
    
    /** 발송 시간 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ChatMessageDto of(ChatRoomMessage entity, ChatUserDto chatUserDto) {
        return ChatMessageDto.builder()
            .messageSequence(entity.getId().getMessageSequence())
            .roomId(entity.getId().getRoomId())
            .type(entity.getType())
            .content(entity.getContent())
            .senderId(entity.getSenderId())
            .timestamp(entity.getTimestamp())
            .chatUserDto(chatUserDto)
            .build();
    }
}