package com.example.chat.ingestion.extraction;

import com.example.chat.ingestion.model.Document;
import com.example.chat.ingestion.model.ExtractedContent;
import org.springframework.stereotype.Component;


@Component
public class PdfTextExtractor implements TextExtractor {
    
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    @Override
    public boolean supports(String contentType) {
        return PDF_CONTENT_TYPE.equals(contentType);
    }
    
    @Override
    public ExtractedContent extract(Document document) throws ExtractionException {
      return null;
    }
}