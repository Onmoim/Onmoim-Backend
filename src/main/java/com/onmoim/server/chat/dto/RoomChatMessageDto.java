package com.onmoim.server.chat.dto;

import java.time.LocalDateTime;

import com.onmoim.server.chat.entity.ChatRoomMessage;
import com.onmoim.server.chat.entity.MessageType;
import com.onmoim.server.chat.entity.ChatRoomMessageId;

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
public class RoomChatMessageDto {
    
    /** 메시지 고유 ID */
    private ChatRoomMessageId messageId;
    
    /** 채팅방 ID */
    private Long roomId;
    
    /** 메시지 타입 */
    private MessageType type;
    
    /** 메시지 내용 */
    private String content;
    
    /** 발신자 ID (서버에서 Principal 기반으로 설정) */
    private String senderId;
    
    /** 발신자 이름 (UI 표시용) */
    private String senderName;
    
    /** 세션 ID */
    private String sessionId;
    
    /** 발송 시간 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static RoomChatMessageDto from(ChatRoomMessage entity) {
        return RoomChatMessageDto.builder()
            .messageId(entity.getId())
            .type(entity.getType())
            .content(entity.getContent())
            .senderId(entity.getSenderId())
            .timestamp(entity.getTimestamp())
            .build();
    }
}