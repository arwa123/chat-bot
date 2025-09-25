package com.example.chat.ingestion.document;

import com.example.chat.ingestion.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of DocumentProcessor for PDF files.
 */
@Component
public class PdfDocumentProcessor implements DocumentProcessor {

    @Override
    public boolean supports(String contentType) {
        return false;
    }

    @Override
    public Document processFile(MultipartFile file, Map<String, Object> metadata) throws IOException {
        return null;
    }
}