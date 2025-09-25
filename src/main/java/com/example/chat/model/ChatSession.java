package com.example.chat.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_sessions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChatSession {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    private String title;
    @Column(name = "is_favorite")
    private boolean favorite;
    @Column(name = "is_deleted")
    private boolean deleted;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
