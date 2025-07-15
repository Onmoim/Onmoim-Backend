package com.onmoim.server.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onmoim.server.chat.domain.ChatSequence;

public interface ChatSequenceRepository extends JpaRepository<ChatSequence, Long> {
}
