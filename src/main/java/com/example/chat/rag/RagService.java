package com.example.chat.rag;

import com.example.chat.dto.ChatModelDto.RetrievedData;
import com.example.chat.llm.LlmClient;
import com.example.chat.repository.KnowledgeChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagService.class);

    private final KnowledgeChunkRepository knowledgeRepository;
    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;
    private final JdbcTemplate jdbcTemplate;

    public List<RetrievedData> retrieve(String query) {
        List<Double> vec = embeddingClient.getEmbeddings(query);
        String vectorLiteral = toVectorLiteral(vec);
        String sql = """
            SELECT id, content, source, metadata, (embedding <=> ?::vector) AS distance
            FROM knowledge_chunks
            ORDER BY embedding <=> ?::vector
            LIMIT ?
        """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, vectorLiteral, vectorLiteral, 6);
        List<RetrievedData> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            RetrievedData ret =  RetrievedData.builder()
                    .id((UUID) r.get("id"))
                    .content((String) r.get("content"))
                    .source((String) r.get("source"))
                    .metadataJson(r.get("metadata") != null ? r.get("metadata").toString() : null)
                    .score(((Number) r.get("distance")).doubleValue())
                    .build();
            out.add(ret);
        }
        return out;
    }



    public String generateAugmentedAnswer(String userMessage, List<RetrievedData> context) {
        try {
            logger.debug("Generating augmented answer for user message using {} context chunks", context.size());
            
            // Enhanced prompt with better instructions for context utilization
            String promptTemplate = """
                You are a helpful assistant providing accurate information based on the retrieved context. 
                
                Instructions:
                1. Use ONLY the information from the provided context to answer the question
                2. If the exact answer isn't in the context but you can reasonably infer it, do so and explain your reasoning
                3. If the answer cannot be determined from the context, respond with "I don't have enough information to answer that question accurately."
                4. Cite sources when possible by referring to the provided source names
                5. Provide comprehensive answers that fully address the question
                6. When multiple sources provide relevant information, synthesize them into a coherent answer
                
                User question: %s
                
                Context (sorted by relevance):
                %s
                
                Answer:
                """;
            String ctx = context.stream()
                    .map(c -> String.format("- Source: %s (score=%.4f)\n%s", c.source(), c.score(), c.content()))
                    .collect(Collectors.joining("\n\n"));

            String prompt = promptTemplate.formatted(userMessage, ctx);

            logger.debug("Sending enhanced augmented prompt to LLM, length: {}", prompt.length());
            return llmClient.generate(prompt);
        } catch (Exception e) {
            logger.error("Error generating augmented answer", e);
            throw new RuntimeException("Error generating augmented answer: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void insertDataChunk(UUID id, String source, String content, String metadataJson) {
        try {
            logger.debug("Inserted knowledge chunk with ID: {}, source: {}", id, source);

            List<Double> vec = embeddingClient.getEmbeddings(content);
            String vectorLiteral = toVectorLiteral(vec);

            knowledgeRepository.upsertWithEmbedding(id, source, content, metadataJson, vectorLiteral);

            logger.info("Successfully inserted knowledge chunk with ID: {}", id);
        } catch (DataIntegrityViolationException e) {
            logger.error("Vector dimension mismatch while inserted knowledge chunk: {}", e.getMessage());
            throw new RuntimeException("Vector dimension mismatch between embedding client and database schema", e);
        } catch (DataAccessException e) {
            logger.error("Database access error inserted knowledge chunk: {}", e.getMessage());
            throw new RuntimeException("Database error inserted knowledge chunk", e);
        } catch (Exception e) {
            logger.error("Error inserted knowledge chunk", e);
            throw new RuntimeException("Error inserted knowledge chunk: " + e.getMessage(), e);
        }
    }


    private String toVectorLiteral(List<Double> vec) {
        try {
            return "[" + vec.stream()
                    .map(d -> String.format(java.util.Locale.US, "%.6f", d))
                    .collect(Collectors.joining(",")) + "]";
        } catch (Exception e) {
            logger.error("Error creating vector literal", e);
            throw new RuntimeException("Error creating vector literal: " + e.getMessage(), e);
        }
    }
}