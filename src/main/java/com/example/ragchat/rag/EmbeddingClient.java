package com.example.ragchat.rag;

import java.util.List;

public interface EmbeddingClient {
    List<Double> embed(String text, int dim);
}
