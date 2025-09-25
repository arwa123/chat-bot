package com.example.chat.rag;

import com.example.chat.dto.EmbeddingDto.RetrievedKnowledge;
import com.example.chat.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;



    @Value("${rag.retrieval.top-k:3}")
    private int topK;

    /**
     * @deprecated Use {@link com.example.chat.dto.EmbeddingDto.RetrievedKnowledge} instead
     */
    @Deprecated
    public static class Retrieved {
        public UUID id;
        public String content;
        public String source;
        public String metadataJson;
        public double score;
    }

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
     * Retrieve knowledge chunks based on semantic similarity to the query
     * 
     * @param query Text query to find similar chunks for
     * @param limit Maximum number of results to return
     * @return List of retrieved knowledge chunks with similarity scores
     */
    public List<RetrievedKnowledge> retrieve(String query, int limit) {
        try {
            logger.debug("Retrieving knowledge chunks for query: {}", query);
            
            List<Double> vec = embeddingClient.getEmbeddings(query);
            String vectorLiteral = toVectorLiteral(vec);
            
            String sql = """
                SELECT id, content, source, metadata, (embedding <=> ?::vector) AS distance
                FROM knowledge_chunks
                ORDER BY embedding <=> ?::vector
                LIMIT ?
            """;
            
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, vectorLiteral, vectorLiteral, topK);
            logger.debug("Retrieved {} results from knowledge base", rows.size());
            
            List<RetrievedKnowledge> out = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                RetrievedKnowledge ret = new RetrievedKnowledge(
                    (UUID) r.get("id"),
                    (String) r.get("content"),
                    (String) r.get("source"),
                    r.get("metadata") != null ? r.get("metadata").toString() : null,
                    ((Number) r.get("distance")).doubleValue()
                );
                out.add(ret);
            }
            return out;
        } catch (DataIntegrityViolationException e) {
            logger.error("Vector dimension mismatch: {}", e.getMessage());
            throw new RuntimeException("Vector dimension mismatch between embedding client and database schema", e);
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

    public void upsertKnowledgeChunk(UUID id, String source, String content, String metadataJson) {
        try {
            logger.debug("Upserting knowledge chunk with ID: {}, source: {}", id, source);
            
            List<Double> vec = embeddingClient.getEmbeddings(content);
            String vectorLiteral = toVectorLiteral(vec);
            
            String sql = """
                INSERT INTO knowledge_chunks(id, source, content, metadata, embedding)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?::vector)
                ON CONFLICT (id) DO UPDATE SET
                    source = EXCLUDED.source,
                    content = EXCLUDED.content,
                    metadata = EXCLUDED.metadata,
                    embedding = EXCLUDED.embedding
            """;
            
            jdbcTemplate.update(sql, id, source, content, metadataJson, vectorLiteral);
            logger.info("Successfully upserted knowledge chunk with ID: {}", id);
        } catch (DataIntegrityViolationException e) {
            logger.error("Vector dimension mismatch while upserting knowledge chunk: {}", e.getMessage());
            throw new RuntimeException("Vector dimension mismatch between embedding client and database schema", e);
        } catch (Exception e) {
            logger.error("Error upserting knowledge chunk", e);
            throw new RuntimeException("Error upserting knowledge chunk: " + e.getMessage(), e);
        }
    }

    private String toVectorLiteral(List<Double> vec) {
        try {
            return "[" + vec.stream().map(d -> String.format(java.util.Locale.US, "%.6f", d)).collect(Collectors.joining(",")) + "]";
        } catch (Exception e) {
            logger.error("Error creating vector literal", e);
            throw new RuntimeException("Error creating vector literal: " + e.getMessage(), e);
        }
    }
}
