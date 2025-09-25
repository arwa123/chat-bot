package com.example.chat.controller;

import com.example.chat.dto.EmbeddingDto.KnowledgeUpsertRequest;
import com.example.chat.dto.EmbeddingDto.KnowledgeUpsertResponse;
import com.example.chat.dto.EmbeddingDto.KnowledgeRetrievalRequest;
import com.example.chat.dto.EmbeddingDto.KnowledgeRetrievalResponse;
import com.example.chat.dto.LlmDto.GenerationRequest;
import com.example.chat.dto.LlmDto.GenerationResponse;
import com.example.chat.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeController.class);

    private final KnowledgeService knowledgeService;

    @PostMapping("/upsert")
    public ResponseEntity<KnowledgeUpsertResponse> upsert(@Valid @RequestBody KnowledgeUpsertRequest req) {
        logger.info("Processing knowledge upsert request");
        KnowledgeUpsertResponse response = knowledgeService.upsertKnowledge(req);
        
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/retrieve")
    public ResponseEntity<KnowledgeRetrievalResponse> retrieve(@Valid @RequestBody KnowledgeRetrievalRequest req) {
        try {
            logger.info("Processing knowledge retrieval request for query: {}", req.query());
            
            var results = knowledgeService.retrieveKnowledge(req.query(), req.limit());
            
            KnowledgeRetrievalResponse response = new KnowledgeRetrievalResponse(
                results,
                req.query(),
                req.limit()
            );
                
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing knowledge retrieval request", e);
            return ResponseEntity.badRequest().body(
                new KnowledgeRetrievalResponse(
                    null,
                    req.query(),
                    req.limit()
                )
            );
        }
    }
    
    @PostMapping("/generate")
    public ResponseEntity<GenerationResponse> generate(@Valid @RequestBody GenerationRequest req) {
        try {
            logger.info("Processing generation request with context size: {}", 
                    req.context() != null ? req.context().size() : 0);
            
            GenerationResponse response = knowledgeService.generateAnswer(req);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing generation request", e);
            return ResponseEntity.badRequest().body(
                GenerationResponse.error("Error: " + e.getMessage())
            );
        }
    }
}
