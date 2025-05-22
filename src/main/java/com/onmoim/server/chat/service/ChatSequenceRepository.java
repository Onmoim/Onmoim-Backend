package com.onmoim.server.chat.service;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.entity.ChatSequence;

public interface ChatSequenceRepository extends JpaRepository<ChatSequence, Long> {
}
