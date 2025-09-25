package com.example.chat.ingestion.document;

import com.example.chat.ingestion.model.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for creating Document objects from different file sources.
 * Implementations will handle specific file types.
 */
public interface DocumentProcessor {

    boolean supports(String contentType);

    Document processFile(MultipartFile file, Map<String, Object> metadata) throws IOException;
}