package com.example.ragchat.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "knowledge_chunks")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class KnowledgeChunk {
    @Id
    private UUID id;
    private String source;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    @Column(columnDefinition = "jsonb")
    private String metadata;
    // embedding stored in Postgres vector column; not mapped in JPA
}
