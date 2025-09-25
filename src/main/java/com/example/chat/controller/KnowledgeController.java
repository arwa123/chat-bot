package com.example.chat.controller;

import com.example.chat.rag.RagService;
import com.example.chat.dto.Dtos.KnowledgeUpsertReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final RagService ragService;

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(@RequestBody KnowledgeUpsertReq req) {
        UUID id = req.id == null || req.id.isBlank() ? UUID.randomUUID() : UUID.fromString(req.id);
        String metadataJson = req.metadata == null ? null : req.metadata.toString();
        ragService.upsertKnowledgeChunk(id, req.source, req.content, metadataJson);
        return ResponseEntity.ok(Map.of("id", id));
    }
}
