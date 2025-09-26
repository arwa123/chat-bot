package com.example.chat.ingestion.extraction;

import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.model.ExtractedContent;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class TxtTextExtractor implements TextExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(TxtTextExtractor.class);
    private static final String TXT_CONTENT_TYPE = "text/plain";
    
    @Override
    public boolean supports(String contentType) {
        return TXT_CONTENT_TYPE.equals(contentType);
    }
    
    @Override
    public ExtractedContent extract(Document document) throws ExtractionException {
        try {
            logger.info("Extracting text from plain text document: {}", document.filename());
            
            String text = IOUtils.toString(document.content(), StandardCharsets.UTF_8);
            Map<String, Object> metadata = new HashMap<>(document.metadata());
            metadata.put("character_count", text.length());
            metadata.put("line_count", countLines(text));
            
            logger.debug("Extracted {} characters from text file", text.length());
            
            return ExtractedContent.builder()
                    .id(document.id())
                    .source(document.filename())
                    .content(text)
                    .metadata(metadata)
                    .build();
        } catch (IOException e) {
            logger.error("Error extracting text from plain text file: {}", e.getMessage(), e);
            throw new ExtractionException("Failed to extract text from plain text document: " + e.getMessage(), e);
        }
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        return text.split("\r\n|\r|\n").length;
    }
}