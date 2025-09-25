package com.example.chat.service;

import com.example.chat.dto.ChatModelDto.KnowledgeUpsertRequest;
import com.example.chat.dto.ChatModelDto.KnowledgeUpsertResponse;
import com.example.chat.dto.ChatModelDto.RetrievedKnowledge;
import com.example.chat.dto.LLMDto.GenerationRequest;
import com.example.chat.dto.LLMDto.GenerationResponse;
import com.example.chat.llm.LlmClient;
import com.example.chat.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing knowledge and generating answers using RAG
 */
@Service
@RequiredArgsConstructor
public class KnowledgeService {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeService.class);
    
    private final RagService ragService;
    private final LlmClient llmClient;
    
    /**
     * Store or update a knowledge chunk
     * 
     * @param request Knowledge chunk data to store
     * @return Response with operation status and chunk ID
     */
    public KnowledgeUpsertResponse upsertKnowledge(KnowledgeUpsertRequest request) {
        try {
            logger.info("Upserting knowledge chunk");
            
            UUID id = request.id() == null || request.id().isBlank() ? 
                UUID.randomUUID() : UUID.fromString(request.id());
                
            String metadataJson = request.metadata() == null ? null : request.metadata().toString();
            
            ragService.upsertKnowledgeChunk(id, request.source(), request.content(), metadataJson);
            
            return KnowledgeUpsertResponse.success(id, "Knowledge chunk successfully stored");
                
        } catch (Exception e) {
            logger.error("Error upserting knowledge chunk", e);
            
            return KnowledgeUpsertResponse.error("Error: " + e.getMessage());
        }
    }
    
    /**
     * Retrieve knowledge chunks related to the query
     * 
     * @param query The search query
     * @param limit Maximum number of results to return
     * @return List of knowledge chunks sorted by relevance
     */
    public List<RetrievedKnowledge> retrieveKnowledge(String query, int limit) {
        logger.info("Retrieving knowledge for query: {}, limit: {}", query, limit);
        return ragService.retrieve(query, limit);
    }
    
    /**
     * Generate an answer using retrieved knowledge context
     * 
     * @param request Generation request with prompt and context
     * @return Generated text response
     */
    public GenerationResponse generateAnswer(GenerationRequest request) {
        try {
            logger.info("Generating answer with RAG using {} context chunks", 
                    request.context() != null ? request.context().size() : 0);
            
            String answer = ragService.generateAugmentedAnswer(request.prompt(), request.context());
            
            return GenerationResponse.success(
                answer,
                llmClient.getClass().getSimpleName(),
                request.context() != null && !request.context().isEmpty()
            );
                
        } catch (Exception e) {
            logger.error("Error generating answer", e);
            
            return GenerationResponse.error("Error generating answer: " + e.getMessage());
        }
    }
}