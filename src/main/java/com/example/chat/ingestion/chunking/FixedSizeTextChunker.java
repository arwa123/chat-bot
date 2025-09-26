package com.example.chat.ingestion.chunking;

import com.example.chat.ingestion.model.ExtractedContent;
import com.example.chat.ingestion.model.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FixedSizeTextChunker implements TextChunker {

    private static final Logger logger = LoggerFactory.getLogger(FixedSizeTextChunker.class);

    private final int defaultChunkSize;
    private final int defaultOverlapSize;

    public FixedSizeTextChunker(
            @Value("${rag.chunking.default-chunk-size:1000}") int defaultChunkSize,
            @Value("${rag.chunking.default-overlap-size:100}") int defaultOverlapSize) {
        this.defaultChunkSize = defaultChunkSize;
        this.defaultOverlapSize = defaultOverlapSize;
        
        logger.info("Initialized fixed-size text chunker with chunk size {}, overlap {}",
                defaultChunkSize, defaultOverlapSize);
    }

    @Override
    public String getType() {
        return "fixed-size";
    }

    @Override
    public List<TextChunk> chunk(ExtractedContent content) {
        return chunk(content, defaultChunkSize, defaultOverlapSize);
    }

    public List<TextChunk> chunk(ExtractedContent content, int chunkSize, int overlapSize) {
        String text = content.content();

        if (text == null || text.isEmpty()) {
            logger.warn("Empty text provided for chunking");
            return Collections.emptyList();
        }

        if (chunkSize <= 0) {
            chunkSize = defaultChunkSize;
        }

        if (overlapSize < 0 || overlapSize >= chunkSize) {
            overlapSize = Math.min(defaultOverlapSize, chunkSize / 2);
        }

        logger.debug("Chunking text of length {} with chunk size {} and overlap {}",
                text.length(), chunkSize, overlapSize);

        List<TextChunk> chunks = new ArrayList<>();
        int textLength = text.length();

        if (textLength <= chunkSize) {
            chunks.add(createChunk(content, text, 0));
            return chunks;
        }

        int position = 0;
        int chunkIndex = 0;
        
        while (position < textLength) {
            int end = Math.min(position + chunkSize, textLength);
            String chunkText = text.substring(position, end);
            chunks.add(createChunk(content, chunkText, chunkIndex++));
            position += (chunkSize - overlapSize);
            if (position >= textLength) {
                break;
            }
        }

        logger.info("Created {} chunks from text of length {}", chunks.size(), textLength);
        return chunks;
    }

    private TextChunk createChunk(ExtractedContent content, String chunkText, int chunkIndex) {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("document_id", content.id().toString());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_text_length", chunkText.length());

        if (content.metadata() != null && content.metadata().containsKey("source_type")) {
            metadata.put("source_type", content.metadata().get("source_type"));
        }

        return TextChunk.builder()
                .id(UUID.randomUUID())
                .documentId(content.id())
                .source(content.source())
                .content(chunkText)
                .metadata(metadata)
                .chunkIndex(chunkIndex)
                .build();
    }
}