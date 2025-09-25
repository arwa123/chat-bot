package com.example.ragchat.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;
    private final LlmClient llmClient;

    @Value("${rag.embedding.dimension:384}")
    private int dimension;

    @Value("${rag.retrieval.top-k:3}")
    private int topK;

    public static class Retrieved {
        public UUID id;
        public String content;
        public String source;
        public String metadataJson;
        public double score;
    }

    public List<Retrieved> retrieve(String query) {
        List<Double> vec = embeddingClient.embed(query, dimension);
        String vectorLiteral = toVectorLiteral(vec);
        String sql = """
            SELECT id, content, source, metadata, (embedding <=> ?::vector) AS distance
            FROM knowledge_chunks
            ORDER BY embedding <=> ?::vector
            LIMIT ?
        """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, vectorLiteral, vectorLiteral, topK);
        List<Retrieved> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Retrieved ret = new Retrieved();
            ret.id = (UUID) r.get("id");
            ret.content = (String) r.get("content");
            ret.source = (String) r.get("source");
            ret.metadataJson = r.get("metadata") != null ? r.get("metadata").toString() : null;
            ret.score = ((Number) r.get("distance")).doubleValue();
            out.add(ret);
        }
        return out;
    }

    public String generateAugmentedAnswer(String userMessage, List<Retrieved> context) {
        String ctx = context.stream()
                .map(c -> String.format("- Source: %s (score=%.4f)\n%s", c.source, c.score, c.content))
                .collect(Collectors.joining("\n\n"));
        String prompt = """
You are a helpful assistant. Answer the user's question using ONLY the provided context. If the answer is not in the context, say you don't know.
User question:
%s

Context:
%s
""".formatted(userMessage, ctx);
        return llmClient.generate(prompt);
    }

    public void upsertKnowledgeChunk(UUID id, String source, String content, String metadataJson) {
        List<Double> vec = embeddingClient.embed(content, dimension);
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
    }

    private String toVectorLiteral(List<Double> vec) {
        return "[" + vec.stream().map(d -> String.format(java.util.Locale.US, "%.6f", d)).collect(Collectors.joining(",")) + "]";
    }
}
