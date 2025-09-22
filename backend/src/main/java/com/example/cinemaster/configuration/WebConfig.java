package com.example.cinemaster.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // Cho phép gọi từ các frontend (Live Server, React, Angular, IntelliJ)
                        .allowedOrigins(
                                "http://127.0.0.1:5500",
                                "http://localhost:5500",
                                "http://localhost:63342",
                                "http://localhost:3000",
                                "http://localhost:4200"
                        )
                        // Các method HTTP được phép
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        // Cho phép gửi header tùy chỉnh (JWT, Content-Type, Authorization, …)
                        .allowedHeaders("*")

                        // Không bật cookie/session trong giai đoạn dev
                        .allowCredentials(false);
            }
        };
    }
}
