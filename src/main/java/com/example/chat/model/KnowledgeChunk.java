package com.example.chat.model;

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
    // The embedding field is not directly mapped by JPA as it's a Postgres-specific vector type
    // We'll handle it through native queries
}
