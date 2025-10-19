package com.example.cinemaster.entity;

import java.util.List;

public class DocumentChunk {
    private String id;
    private String content; // Văn bản gốc
    private List<Double> embedding; // Vector
    private String source; // Nguồn dữ liệu (ví dụ: "FAQ", "Policy")
    private double score; // Dùng tạm để lưu điểm tương đồng khi tìm kiếm

    // Constructors (Thêm constructor với score để tiện cho việc ranking)
    public DocumentChunk() {}

    public DocumentChunk(String id, String content, List<Double> embedding, String source) {
        this.id = id;
        this.content = content;
        this.embedding = embedding;
        this.source = source;
    }

    public DocumentChunk(String id, String content, List<Double> embedding, String source, double score) {
        this.id = id;
        this.content = content;
        this.embedding = embedding;
        this.source = source;
        this.score = score;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
