package com.example.chat.ingestion.chunking;

/**
 * Configuration parameters for text chunking.
 * Different implementations can use different subsets of these parameters.
 */
public class ChunkingConfig {
    private final int maxChunkSize;
    private final int overlapSize;
    private final String chunkType;

    private ChunkingConfig(int maxChunkSize, int overlapSize, String chunkType) {
        this.maxChunkSize = maxChunkSize;
        this.overlapSize = overlapSize;
        this.chunkType = chunkType;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public int getOverlapSize() {
        return overlapSize;
    }

    public String getChunkType() {
        return chunkType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxChunkSize = 1000;
        private int overlapSize = 100;
        private String chunkType = "fixed-size";

        public Builder maxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
            return this;
        }

        public Builder overlapSize(int overlapSize) {
            this.overlapSize = overlapSize;
            return this;
        }

        public Builder chunkType(String chunkType) {
            this.chunkType = chunkType;
            return this;
        }

        public ChunkingConfig build() {
            return new ChunkingConfig(maxChunkSize, overlapSize, chunkType);
        }
    }
}