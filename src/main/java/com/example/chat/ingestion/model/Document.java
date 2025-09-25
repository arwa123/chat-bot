package com.example.chat.ingestion.model;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a document in the data ingestion pipeline.
 * Documents contain metadata and the actual content as an InputStream.
 */
public class Document {
    private final UUID id;
    private final String filename;
    private final String contentType;
    private final Map<String, Object> metadata;
    private final InputStream content;

    private Document(UUID id, String filename, String contentType, Map<String, Object> metadata, InputStream content) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.metadata = metadata;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public InputStream getContent() {
        return content;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String filename;
        private String contentType;
        private Map<String, Object> metadata;
        private InputStream content;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder content(InputStream content) {
            this.content = content;
            return this;
        }

        public Document build() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            
            return new Document(id, filename, contentType, metadata, content);
        }
    }
}