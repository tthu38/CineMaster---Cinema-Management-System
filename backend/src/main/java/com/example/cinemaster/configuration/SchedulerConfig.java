package com.example.cinemaster.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ⚙️ Cấu hình Scheduler cho các tác vụ tự động
 * - Bật @EnableScheduling để Spring Boot chạy các job định kỳ
 * - Bật @EnableAsync để hỗ trợ chạy đa luồng (nếu cần trong tương lai)
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig {
    // Không cần thêm gì khác — chỉ cần bật annotation là Spring Boot tự quản lý
}
