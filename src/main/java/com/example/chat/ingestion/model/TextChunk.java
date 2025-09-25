package com.example.chat.ingestion.model;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a chunk of text extracted from a document.
 * Each chunk contains a portion of the document's content along with metadata.
 */
public class TextChunk {
    private final UUID id;
    private final UUID documentId;
    private final String source;
    private final String content;
    private final Map<String, Object> metadata;
    private final int chunkIndex;

    private TextChunk(UUID id, UUID documentId, String source, String content, Map<String, Object> metadata, int chunkIndex) {
        this.id = id;
        this.documentId = documentId;
        this.source = source;
        this.content = content;
        this.metadata = metadata;
        this.chunkIndex = chunkIndex;
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

        public TextChunk build() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            
            return new TextChunk(id, documentId, source, content, metadata, chunkIndex);
        }
    }
}