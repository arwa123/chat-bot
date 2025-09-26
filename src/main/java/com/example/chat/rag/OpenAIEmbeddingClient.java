package com.example.chat.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * OpenAI embeddings client implementation.
 * Uses the OpenAI API to generate embeddings for text.
 */
@Component("openaiEmbeddingClient")
public class OpenAIEmbeddingClient implements EmbeddingClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAIEmbeddingClient.class);

    
    @Override
    public List<Double> getEmbeddings(String text) {
        logger.warn("OpenAI embedding client not implemented.");
        throw new UnsupportedOperationException("OpenAI embedding client not implemented.");
    }
}
