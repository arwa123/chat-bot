package com.example.chat.ingestion.model;

import lombok.Builder;
import java.util.List;
import java.util.Map;
import java.util.UUID;



@Builder
public record EmbeddedChunk(UUID id, UUID documentId, String source, String content, Map<String, Object> metadata,
                            int chunkIndex, List<Double> embedding, String embeddingModel) {
}