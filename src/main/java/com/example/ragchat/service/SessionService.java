package com.example.ragchat.service;

import com.example.ragchat.model.ChatSession;
import com.example.ragchat.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final ChatSessionRepository sessionRepo;

    public ChatSession createSession(String userId, String title) {
        ChatSession s = ChatSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title(title == null || title.isBlank() ? "New Chat" : title.trim())
                .favorite(false).deleted(false)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
        return sessionRepo.save(s);
    }

    public Page<ChatSession> listSessions(String userId, int page, int size) {
        return sessionRepo.findByUserIdAndDeletedFalseOrderByUpdatedAtDesc(userId, PageRequest.of(page, size));
    }

    public Optional<ChatSession> get(UUID id) {
        return sessionRepo.findById(id).filter(s -> !s.isDeleted());
    }

    public ChatSession update(UUID id, String title, Boolean favorite) {
        ChatSession s = get(id).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (title != null) s.setTitle(title);
        if (favorite != null) s.setFavorite(favorite);
        s.setUpdatedAt(OffsetDateTime.now());
        return sessionRepo.save(s);
    }

    public void delete(UUID id) {
        ChatSession s = get(id).orElseThrow(() -> new IllegalArgumentException("Session not found"));
        s.setDeleted(true);
        s.setUpdatedAt(OffsetDateTime.now());
        sessionRepo.save(s);
    }
}
