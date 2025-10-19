package com.example.cinemaster.dto.response;

import java.util.List;

public class GeminiResponse {
    private List<Candidate> candidates;

    // Getters and Setters
    public List<Candidate> getCandidates() { return candidates; }
    public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

    public static class Candidate {
        private Content content;

        // Getter and Setter
        public Content getContent() { return content; }
        public void setContent(Content content) { this.content = content; }
    }

    public static class Content {
        private List<Part> parts;

        // Getter and Setter
        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
    }

    public static class Part {
        private String text;

        // Getter and Setter
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    // Phương thức tiện ích để trích xuất câu trả lời
    public String getFirstResponseText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.getContent() != null && candidate.getContent().getParts() != null && !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return "Không thể trích xuất câu trả lời từ AI.";
    }
}