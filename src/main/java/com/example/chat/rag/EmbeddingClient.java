package com.example.chat.rag;

import java.util.List;

public interface EmbeddingClient {
    List<Double> getEmbeddings(String text);
}
