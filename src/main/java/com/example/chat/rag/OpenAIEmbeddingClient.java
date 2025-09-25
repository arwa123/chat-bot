package com.example.chat.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

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
