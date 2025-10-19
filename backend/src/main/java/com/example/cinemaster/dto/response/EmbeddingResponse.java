package com.example.cinemaster.dto.response;

import java.util.List;

public class EmbeddingResponse {
    // Gemini trả về 1 đối tượng "embedding" duy nhất
    private Embedding embedding;

    // Getter/Setter
    public Embedding getEmbedding() { return embedding; }
    public void setEmbedding(Embedding embedding) { this.embedding = embedding; }

    // Tiện ích lấy giá trị vector
    public List<Double> getFirstEmbeddingValues() {
        return embedding != null ? embedding.getValues() : List.of();
    }

    public static class Embedding {
        private List<Double> values;

        public List<Double> getValues() { return values; }
        public void setValues(List<Double> values) { this.values = values; }
    }
}
