package com.example.chat.ingestion.model;

import lombok.Builder;
import java.util.Map;
import java.util.UUID;


@Builder
public record ExtractedContent(UUID id, String source, String content, Map<String, Object> metadata) {
}