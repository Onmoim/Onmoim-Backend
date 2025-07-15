package com.onmoim.server.chat.domain;

import com.onmoim.server.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long groupId;

    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;
}