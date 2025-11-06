package com.example.cinemaster.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeminiRequest {
    private List<Content> contents;


    @JsonProperty("systemInstruction")
    private Content systemInstruction;
    @JsonProperty("generationConfig")
    private GenerateContentConfig generationConfig;

    public GeminiRequest(List<Content> contents, Content systemInstruction) {
        this.contents = contents;
        this.systemInstruction = systemInstruction; // Gán đối tượng Content vào đây
        this.generationConfig = new GenerateContentConfig(0.2f);
    }

    public GeminiRequest(List<Content> contents) {
        this.contents = contents;
        this.systemInstruction = null;
        this.generationConfig = new GenerateContentConfig(0.2f);
    }



    public List<Content> getContents() { return contents; }
    public void setContents(List<Content> contents) { this.contents = contents; }
    public GenerateContentConfig getGenerationConfig() { return generationConfig; }
    public void setGenerationConfig(GenerateContentConfig generationConfig) { this.generationConfig = generationConfig; }
    // *** ĐIỂM SỬA LỖI 2: Getter và Setter phải dùng kiểu Content ***
    public Content getSystemInstruction() { return systemInstruction; }
    public void setSystemInstruction(Content systemInstruction) { this.systemInstruction = systemInstruction; }


    // --- INNER STATIC CLASSES ---

    // Lưu ý: Các lớp này giờ là Inner Static Class trong file của bạn
    public static class Content {
        private String role;
        private List<Part> parts;

        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public List<Part> getParts() { return parts; }
        public void setParts(List<Part> parts) { this.parts = parts; }
    }

    public static class Part {
        private String text;

        public Part(String text) {
            this.text = text;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class GenerateContentConfig {
        private float temperature;
        @JsonProperty("maxOutputTokens")
        private int maxOutputTokens = 1024;

        @JsonProperty("response_mime_type")
        private String responseMimeType = "application/json";

        public GenerateContentConfig(float temperature) {
            this.temperature = temperature;
        }
        public GenerateContentConfig(float temperature, int tokenLimit, String mimeType) {
            this.temperature = temperature;
            this.maxOutputTokens = tokenLimit;
            this.responseMimeType = mimeType;
        }

        public float getTemperature() { return temperature; }
        public void setTemperature(float temperature) { this.temperature = temperature; }
    }
}