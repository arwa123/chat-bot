package com.example.chat.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTOs for embedding and knowledge base operations
 * Using Java Records for immutable data transfer objects
 */
public class EmbeddingDto {

    /**
     * Request to upsert a knowledge chunk
     */
    public record KnowledgeUpsertRequest(
        String id, // Optional UUID; if omitted server will create
        String source,
        String content,
        Object metadata
    ) {}

    /**
     * Response after upserting a knowledge chunk
     */
    public record KnowledgeUpsertResponse(
        UUID id,
        boolean success,
        String message
    ) {
        // Static factory methods to replace builders
        public static KnowledgeUpsertResponse success(UUID id, String message) {
            return new KnowledgeUpsertResponse(id, true, message);
        }
        
        public static KnowledgeUpsertResponse error(String message) {
            return new KnowledgeUpsertResponse(null, false, message);
        }
    }

    /**
     * Request to retrieve knowledge chunks based on a query
     */
    public record KnowledgeRetrievalRequest(
        String query,
        int limit
    ) {}

    /**
     * Represents a retrieved knowledge chunk with similarity score
     */
    public record RetrievedKnowledge(
        UUID id,
        String content,
        String source,
        String metadataJson,
        double score
    ) {}

    /**
     * Response containing retrieved knowledge chunks
     */
    public record KnowledgeRetrievalResponse(
        List<RetrievedKnowledge> results,
        String query,
        int limit
    ) {}
}