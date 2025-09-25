package com.example.ragchat.repository;

import com.example.ragchat.model.ChatMessage;
import com.example.ragchat.model.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    Page<ChatMessage> findBySessionOrderByCreatedAtAsc(ChatSession session, Pageable pageable);
}
