package com.example.chat.ingestion.document;

import com.example.chat.ingestion.model.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface DocumentProcessor {

    boolean supports(String contentType);

    Document processFile(MultipartFile file, Map<String, Object> metadata) throws IOException;
}