package com.example.cinemaster.dto.request;

import java.util.List;

public class EmbeddingRequest {
    // Trường ngoài cùng là "content" (số ít)
    private Content content;

    public EmbeddingRequest(String text) {
        this.content = new Content(text);
    }

    // Getters and Setters
    public Content getContent() { return content; }
    public void setContent(Content content) { this.content = content; }

    public static class Content {
        // Bên trong có trường "parts"
        private List<Part> parts;

        public Content(String text) {
            this.parts = List.of(new Part(text));
        }

        // Getters and Setters
        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
    }

    public static class Part {
        // Mỗi "part" có trường "text"
        private String text;

        public Part(String text) {
            this.text = text;
        }

        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}