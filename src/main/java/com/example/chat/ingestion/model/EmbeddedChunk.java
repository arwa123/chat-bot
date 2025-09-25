package com.example.chat.ingestion.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a text chunk with its vector embedding.
 * This is the final result after embedding that will be stored in the vector database.
 */
public class EmbeddedChunk {
    private final UUID id;
    private final UUID documentId;
    private final String source;
    private final String content;
    private final Map<String, Object> metadata;
    private final int chunkIndex;
    private final List<Double> embedding;
    private final String embeddingModel;

    private EmbeddedChunk(UUID id, UUID documentId, String source, String content, 
                          Map<String, Object> metadata, int chunkIndex,
                          List<Double> embedding, String embeddingModel) {
        this.id = id;
        this.documentId = documentId;
        this.source = source;
        this.content = content;
        this.metadata = metadata;
        this.chunkIndex = chunkIndex;
        this.embedding = embedding;
        this.embeddingModel = embeddingModel;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID documentId;
        private String source;
        private String content;
        private Map<String, Object> metadata;
        private int chunkIndex;
        private List<Double> embedding;
        private String embeddingModel;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder documentId(UUID documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder chunkIndex(int chunkIndex) {
            this.chunkIndex = chunkIndex;
            return this;
        }

        public Builder embedding(List<Double> embedding) {
            this.embedding = embedding;
            return this;
        }

        public Builder embeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }

        public EmbeddedChunk build() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            
            return new EmbeddedChunk(id, documentId, source, content, 
                                     metadata, chunkIndex, embedding, embeddingModel);
        }
    }
}