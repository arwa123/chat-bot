package com.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Legacy DTOs converted to records for immutability
 */
public class ChatModelDto {

    public record CreateSessionReq(String title, String userId) {}

    public record UpdateSessionReq(String title, Boolean isFavorite) {}

    public record CreateMessageReq(
        @NotNull String sender, // user|assistant|system|tool
        @NotBlank String content,
        Object context,
        boolean generate, // if true and sender=user, run RAG + generate assistant reply
        String userId // who owns the session (for access)
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
}
