package com.example.chat.ingestion.chunking;

import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.model.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.print.Doc;
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
    public List<TextChunk> chunk(Document document) {
        return chunk(document, defaultChunkSize, defaultOverlapSize);
    }

    public List<TextChunk> chunk(Document document, int chunkSize, int overlapSize) {
        String text = document.content();
        List<TextChunk> chunks = new ArrayList<>();
        int textLength = text.length();
        if (textLength <= chunkSize) {
            chunks.add(createChunk(document, text, 0));
            return chunks;
        }
        int position = 0;
        int chunkIndex = 0;
        while (position < textLength) {
            int end = Math.min(position + chunkSize, textLength);
            String chunkText = text.substring(position, end);
            chunks.add(createChunk(document, chunkText, chunkIndex++));
            position += (chunkSize - overlapSize);
            if (position >= textLength) {
                break;
            }
        }
        return chunks;
    }

    private TextChunk createChunk(Document document, String chunkText, int chunkIndex) {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("document_id", document.id().toString());
        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_text_length", chunkText.length());

        if (document.metadata() != null && document.metadata().containsKey("source_type")) {
            metadata.put("source_type", document.metadata().get("source_type"));
        }

        return TextChunk.builder()
                .id(UUID.randomUUID())
                .documentId(document.id())
                .source(document.filename())
                .content(chunkText)
                .metadata(metadata)
                .chunkIndex(chunkIndex)
                .build();
    }
}