package com.example.cinemaster.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * Tạo Bean RestTemplate để sử dụng cho việc gọi API ngoài (như Gemini API).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}