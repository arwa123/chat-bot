package com.example.chat.repository;

import com.example.chat.model.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    Page<ChatSession> findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(String userId, Pageable pageable);
}
