package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class KnowledgeChunkWithDistance {
    private UUID id;
    private String content;
    private String source;
    private String metadata;
    private Double distance;
    
    /**
     * Convert to domain model
     */
    public KnowledgeChunk toKnowledgeChunk() {
        return KnowledgeChunk.builder()
                .id(id)
                .content(content)
                .source(source)
                .metadata(metadata)
                .build();
    }
}