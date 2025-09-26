package com.example.chat.ingestion.document;

import com.example.chat.ingestion.model.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;


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