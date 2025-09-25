package com.example.chat.rag;

import java.util.List;

/**
 * Interface for embedding text into vector representations.
 * Implementations should provide consistent vector dimensions
 * that match with the vector database schema.
 */
public interface EmbeddingClient {
    /**
     * Generate embedding vector for a single text
     * @param text Text to embed
     * @return Vector representation as a list of doubles
     */
    List<Double> getEmbeddings(String text);
}
