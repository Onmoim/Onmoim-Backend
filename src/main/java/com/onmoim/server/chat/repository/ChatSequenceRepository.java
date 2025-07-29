package com.onmoim.server.chat.repository;

import com.onmoim.server.chat.domain.ChatSequence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSequenceRepository extends JpaRepository<ChatSequence, Long> {
}
