package com.example.chat.ingestion.factory;

import com.example.chat.ingestion.chunking.FixedSizeTextChunker;
import com.example.chat.ingestion.chunking.TextChunker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TextChunkerFactory {
    

    @Autowired
    FixedSizeTextChunker fixedSizeTextChunker;

    public TextChunker getChunker() {
       return fixedSizeTextChunker;
    }
    

}