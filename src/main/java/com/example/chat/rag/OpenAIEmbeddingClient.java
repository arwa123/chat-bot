package com.example.chat.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

// Placeholder showing where to call OpenAI embeddings API
@Component("openaiEmbeddingClient")
public class OpenAIEmbeddingClient implements EmbeddingClient {

    @Value("${rag.embedding.openai-api-key:}")
    private String apiKey;

    @Override
    public List<Double> getEmbeddings(String text) {
        throw new UnsupportedOperationException("OpenAI embedding client not implemented in this starter. Use stub or implement API call.");
    }
}
