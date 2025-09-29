package com.example.chat.ingestion.model;

import lombok.*;
import java.util.Map;
import java.util.UUID;


@Builder
public record Document(UUID id, String filename, String contentType, Map<String, Object> metadata,
                       String content) {
}