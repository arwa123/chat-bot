package com.example.chat.ingestion.storage;

import com.example.chat.ingestion.model.EmbeddedChunk;

/**
 * Represents a search result from a vector similarity search.
 * Contains the chunk and its similarity score relative to the query.
 */
public class SimilaritySearchResult {
    private final EmbeddedChunk chunk;
    private final double score;

    public SimilaritySearchResult(EmbeddedChunk chunk, double score) {
        this.chunk = chunk;
        this.score = score;
    }

    /**
     * Get the embedded chunk that matched the query
     * 
     * @return The embedded chunk
     */
    public EmbeddedChunk getChunk() {
        return chunk;
    }

    /**
     * Get the similarity score between the chunk and the query
     * Higher values indicate more similarity, with the exact range depending on the distance metric used.
     * 
     * @return The similarity score
     */
    public double getScore() {
        return score;
    }
}