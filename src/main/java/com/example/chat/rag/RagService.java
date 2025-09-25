package com.example.chat.rag;

import com.example.chat.dto.ChatModelDto.RetrievedKnowledge;
import com.example.chat.llm.LlmClient;
import com.example.chat.model.KnowledgeChunkWithDistance;
import com.example.chat.repository.KnowledgeChunkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Retrieval Augmented Generation (RAG)
 * Handles vector search and LLM generation with context
 */
@Service
@RequiredArgsConstructor
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final KnowledgeChunkRepository knowledgeRepository;
    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;

    @Value("${rag.retrieval.top-k:3}")
    private int topK;

    /**
     * Retrieve knowledge chunks based on semantic similarity to the query
     * 
     * @param query Text query to find similar chunks for
     * @return List of retrieved knowledge chunks with similarity scores
     */
    public List<RetrievedKnowledge> retrieve(String query) {
        return retrieve(query, topK);
    }
    
    /**
     * Retrieve knowledge chunks using vector similarity search
     * 
     * @param query The text query to find similar chunks for
     * @param limit Maximum number of results to return
     * @return List of retrieved knowledge chunks with similarity scores
     */
    @Transactional(readOnly = true)
    public List<RetrievedKnowledge> retrieve(String query, int limit) {
        try {
            logger.debug("Retrieving knowledge chunks for query: {}", query);
            
            // Generate embeddings for the query
            List<Double> vec = embeddingClient.getEmbeddings(query);
            String vectorLiteral = toVectorLiteral(vec);

            List<KnowledgeChunkWithDistance> results = knowledgeRepository.findSimilarByVector(vectorLiteral, limit);
            logger.debug("Retrieved {} results from knowledge base", results.size());

            return results.stream()
                .map(chunk -> new RetrievedKnowledge(
                    chunk.getId(),
                    chunk.getContent(),
                    chunk.getSource(),
                    chunk.getMetadata(),
                    chunk.getDistance()
                ))
                .collect(Collectors.toList());
                
        } catch (DataIntegrityViolationException e) {
            logger.error("Vector dimension mismatch: {}", e.getMessage());
            throw new RuntimeException("Vector dimension mismatch between embedding client and database schema", e);
        } catch (DataAccessException e) {
            logger.error("Database access error retrieving knowledge chunks: {}", e.getMessage());
            throw new RuntimeException("Database error retrieving knowledge chunks", e);
        } catch (Exception e) {
            logger.error("Error retrieving knowledge chunks", e);
            throw new RuntimeException("Error retrieving knowledge chunks: " + e.getMessage(), e);
        }
    }

    /**
     * Generate an answer to the user's message using retrieved knowledge context
     * 
     * @param userMessage The user's question or message
     * @param context List of retrieved knowledge chunks to use as context
     * @return Generated answer that incorporates the provided context
     */
    public String generateAugmentedAnswer(String userMessage, List<RetrievedKnowledge> context) {
        try {
            logger.debug("Generating augmented answer for user message using {} context chunks", context.size());
            
            String ctx = context.stream()
                    .map(c -> String.format("- Source: %s (score=%.4f)\n%s", 
                            c.source(), c.score(), c.content()))
                    .collect(Collectors.joining("\n\n"));
            
            String prompt = """
You are a helpful assistant. Answer the user's question using ONLY the provided context. If the answer is not in the context, say you don't know.
User question:
%s

Context:
%s
""".formatted(userMessage, ctx);
            
            logger.debug("Sending augmented prompt to LLM, length: {}", prompt.length());
            return llmClient.generate(prompt);
        } catch (Exception e) {
            logger.error("Error generating augmented answer", e);
            throw new RuntimeException("Error generating augmented answer: " + e.getMessage(), e);
        }
    }

    /**
     * Insert or update a knowledge chunk with its vector embedding
     * 
     * @param id Unique identifier for the chunk
     * @param source Source of the knowledge (e.g., document name, URL)
     * @param content Text content of the chunk
     * @param metadataJson Optional metadata as JSON string
     */
    @Transactional
    public void upsertKnowledgeChunk(UUID id, String source, String content, String metadataJson) {
        try {
            logger.debug("Upserting knowledge chunk with ID: {}, source: {}", id, source);
            
            // Generate embeddings for the content
            List<Double> vec = embeddingClient.getEmbeddings(content);
            String vectorLiteral = toVectorLiteral(vec);
            
            // Insert or update the knowledge chunk with its embedding
            knowledgeRepository.upsertWithEmbedding(id, source, content, metadataJson, vectorLiteral);
            
            logger.info("Successfully upserted knowledge chunk with ID: {}", id);
        } catch (DataIntegrityViolationException e) {
            logger.error("Vector dimension mismatch while upserting knowledge chunk: {}", e.getMessage());
            throw new RuntimeException("Vector dimension mismatch between embedding client and database schema", e);
        } catch (DataAccessException e) {
            logger.error("Database access error upserting knowledge chunk: {}", e.getMessage());
            throw new RuntimeException("Database error upserting knowledge chunk", e);
        } catch (Exception e) {
            logger.error("Error upserting knowledge chunk", e);
            throw new RuntimeException("Error upserting knowledge chunk: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a list of doubles to a PostgreSQL vector literal string
     * 
     * @param vec List of vector components
     * @return String in PostgreSQL vector format [x,y,z,...]
     */
    private String toVectorLiteral(List<Double> vec) {
        try {
            // Convert to PostgreSQL vector literal format
            return "[" + vec.stream()
                .map(d -> String.format(java.util.Locale.US, "%.6f", d))
                .collect(Collectors.joining(",")) + "]";
        } catch (Exception e) {
            logger.error("Error creating vector literal", e);
            throw new RuntimeException("Error creating vector literal: " + e.getMessage(), e);
        }
    }
}