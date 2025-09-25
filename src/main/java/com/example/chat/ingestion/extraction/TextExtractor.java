package com.example.chat.ingestion.extraction;

import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.model.ExtractedContent;


public interface TextExtractor {
    

    boolean supports(String contentType);
    ExtractedContent extract(Document document) throws ExtractionException;
}