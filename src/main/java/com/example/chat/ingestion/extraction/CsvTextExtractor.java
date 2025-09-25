package com.example.chat.ingestion.extraction;

import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.model.ExtractedContent;
import org.springframework.stereotype.Component;

/**
 * Implementation of TextExtractor for CSV files.
 * Uses OpenCSV to extract and format text from CSV documents.
 */
@Component
public class CsvTextExtractor implements TextExtractor {
    
    private static final String CSV_CONTENT_TYPE = "text/csv";
    private static final String CSV_CONTENT_TYPE_ALT = "application/csv";
    
    @Override
    public boolean supports(String contentType) {
        return CSV_CONTENT_TYPE.equals(contentType) || CSV_CONTENT_TYPE_ALT.equals(contentType);
    }
    
    @Override
    public ExtractedContent extract(Document document) {
        return null;
    }
}