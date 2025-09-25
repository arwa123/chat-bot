package com.example.chat.rag;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Component("stubEmbeddingClient")
public class StubEmbeddingClient implements EmbeddingClient {
    @Override
    public List<Double> getEmbeddings(String text) {
        try {
            int dim = 384;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            List<Double> v = new ArrayList<>(dim);
            for (int i = 0; i < dim; i++) {
                int b = hash[i % hash.length] & 0xff;
                v.add((b / 255.0) - 0.5); // pseudo-embedding normalized around 0
            }
            return v;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
