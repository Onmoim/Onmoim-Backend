package com.onmoim.server.chat.domain;

import java.util.HashSet;
import java.util.Set;

import com.onmoim.server.common.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 엔티티 클래스 - 데이터베이스에 저장
 */
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {
    
    @Id
    private Long id; //groupId와 동일

    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoomMember> chatRoomMembers = new HashSet<>();
    
    public void addMember(ChatRoomMember member) {
        chatRoomMembers.add(member);
        member.setChatRoom(this);
    }
}