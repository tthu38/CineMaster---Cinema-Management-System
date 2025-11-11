package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.EmbeddingRequest;
import com.example.cinemaster.dto.response.EmbeddingResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import java.util.List;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate;
    private final String embeddingModelUrl;

    public EmbeddingService(
            RestTemplate restTemplate,
            @Value("${gemini.api.key}") String geminiApiKey,
            @Value("${gemini.api.url}") String geminiApiUrl,
            @Value("${gemini.embedding.model}") String embeddingModelName
    ) {
        this.restTemplate = restTemplate;
        this.embeddingModelUrl = String.format("%s/%s:embedContent?key=%s", geminiApiUrl, embeddingModelName, geminiApiKey);
    }

    /**
     * Chuyển đổi một đoạn văn bản thành vector (embedding).
     */
    public List<Double> embedText(String text) {
        EmbeddingRequest requestBody = new EmbeddingRequest(text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            EmbeddingResponse response = restTemplate.postForObject(
                    embeddingModelUrl, entity, EmbeddingResponse.class
            );

            if (response != null) {
                return response.getFirstEmbeddingValues();
            }
        } catch (Exception e) {
            System.err.println("Lỗi gọi Gemini Embedding API: " + e.getMessage());
            // Trả về List rỗng nếu lỗi
        }
        return List.of();
    }

    @PostConstruct
    public void testGeminiEmbed() {
        var vec = embedText("giá vé học sinh");
        System.out.println(" Vector length: " + vec.size());
        System.out.println(" Sample dims: " + vec.stream().limit(5).toList());
    }
}
