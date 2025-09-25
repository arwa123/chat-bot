package com.example.chat.ingestion.model;

import java.util.Map;
import java.util.UUID;

/**
 * Represents the extracted plain text content from a document.
 * Contains the original document's metadata plus the extracted text.
 */
public class ExtractedContent {
    private final UUID id;
    private final String source;
    private final String content;
    private final Map<String, Object> metadata;

    private ExtractedContent(UUID id, String source, String content, Map<String, Object> metadata) {
        this.id = id;
        this.source = source;
        this.content = content;
        this.metadata = metadata;
    }

    public UUID getId() {
        return id;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String source;
        private String content;
        private Map<String, Object> metadata;

        public Builder id(UUID id) {
            this.id = id;
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

        public ExtractedContent build() {
            return new ExtractedContent(id, source, content, metadata);
        }
    }
}