package com.onmoim.server.chat.dto;

import com.onmoim.server.chat.entity.ChatRoom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅방 응답 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    
    private Long id;
    private String name;
    private String description;
    private Long creatorId;
    private int memberCount;
    private String subscribeDestination;

    public static ChatRoomResponse fromChatRoom(ChatRoom chatRoom, int memberCount, String subscribeDestination) {
        return ChatRoomResponse.builder()
            .id(chatRoom.getId())
            .name(chatRoom.getName())
            .description(chatRoom.getDescription())
            .creatorId(chatRoom.getCreatorId())
            .memberCount(memberCount)
            .subscribeDestination(subscribeDestination)
            .build();
    }
}