package com.example.cinemaster.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    // ✅ Danh sách endpoint công khai (không yêu cầu xác thực)
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/password/**",
            "/api/v1/chat/**",
            "/uploads/**",
            "/api/v1/merchant/**",
            "/api/v1/vnpay/**",
            "/api/v1/momo/**",
            "/api/v1/sepay/**",
            "/api/v1/trailers/**",
            "/api/v1/payments/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 🔓 Cho phép CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 🔒 Tắt CSRF (vì sử dụng JWT)
                .csrf(csrf -> csrf.disable())
                // 🔥 Giữ context tự động, không cần lưu tay
                .securityContext(context -> context.requireExplicitSave(false))
                .requestCache(cache -> cache.disable())
                // ⚙️ Cấu hình quyền truy cập
                .authorizeHttpRequests(auth -> auth
                        // Cho phép gọi OPTIONS (dành cho preflight request)
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()

                        // ✅ Các endpoint công khai
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // ✅ Cho phép tất cả request GET public (như movie list, showtime, news,...)
                        .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()

                        // ✅ Cho phép gửi form liên hệ (Contact)
                        .requestMatchers(HttpMethod.POST, "/api/v1/contacts/**").permitAll()

                        // ✅ Cho phép tăng lượt xem tin tức (PUT /news/{id}/view)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/news/*/view").permitAll()

                        .requestMatchers("/api/v1/branches/names").permitAll()
                        .requestMatchers("/api/v1/branches/active").permitAll()

                        // ✅ Các API khác phải xác thực
                        .anyRequest().authenticated()
                )
                // 🔐 Thêm JWT filter vào trước UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // 🔐 Mã hóa mật khẩu bằng BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
