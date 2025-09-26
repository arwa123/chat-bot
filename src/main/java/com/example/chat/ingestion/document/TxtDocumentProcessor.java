package com.example.chat.ingestion.document;

import com.example.chat.ingestion.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Component
public class TxtDocumentProcessor implements DocumentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TxtDocumentProcessor.class);
    private static final String TXT_CONTENT_TYPE = "text/plain";

    @Override
    public boolean supports(String contentType) {
        return TXT_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public Document processFile(MultipartFile file, Map<String, Object> metadata) throws IOException {
        logger.info("Processing text file: {}", file.getOriginalFilename());

        if (metadata == null) {
            metadata = new HashMap<>();
        }

        metadata.put("filename", file.getOriginalFilename());
        metadata.put("content_type", file.getContentType());
        metadata.put("size", file.getSize());

        return Document.builder()
                .id(UUID.randomUUID())
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .metadata(metadata)
                .content(file.getInputStream())
                .build();
    }
}