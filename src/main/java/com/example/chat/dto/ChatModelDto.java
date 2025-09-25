package com.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Legacy DTOs converted to records for immutability
 */
public class ChatModelDto {

    public record CreateSessionReq(String title, String userId) {}

    public record UpdateSessionReq(String title, Boolean isFavorite) {}

    public record CreateMessageReq(
        @NotNull String sender,
        @NotBlank String content,
        Object context,
        boolean generate,
        String userId
    ) {}

    public record KnowledgeUpsertReq(
        @NotBlank String content,
        String source,
        Object metadata,
        String id
    ) {}

    public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long total
    ) {}

    public record KnowledgeUpsertRequest(
            String id,
            String source,
            String content,
            Object metadata
    ) {}


    public record KnowledgeUpsertResponse(
            UUID id,
            boolean success,
            String message
    ) {
        public static KnowledgeUpsertResponse success(UUID id, String message) {
            return new KnowledgeUpsertResponse(id, true, message);
        }

        public static KnowledgeUpsertResponse error(String message) {
            return new KnowledgeUpsertResponse(null, false, message);
        }
    }

    public record KnowledgeRetrievalRequest(
            String query,
            int limit
    ) {}


    public record RetrievedKnowledge(
            UUID id,
            String content,
            String source,
            String metadataJson,
            double score
    ) {}

    public record KnowledgeRetrievalResponse(
            List<RetrievedKnowledge> results,
            String query,
            int limit
    ) {}
}
