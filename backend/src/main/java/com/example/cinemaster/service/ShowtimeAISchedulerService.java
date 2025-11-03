package com.example.cinemaster.service;

import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeAISchedulerService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ScreeningPeriodRepository periodRepo;
    private final AuditoriumRepository auditoriumRepo;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String API_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    public List<Map<String, Object>> generateSchedule(Integer branchId, LocalDate date) {
        try {
            List<ScreeningPeriod> periods = periodRepo.findActive(branchId, date);
            List<Auditorium> auditoriums = auditoriumRepo.findActiveByBranch(branchId);

            if (periods.isEmpty() || auditoriums.isEmpty()) {
                log.warn("⚠️ Không có dữ liệu Period hoặc Auditorium hợp lệ để tạo lịch.");
                return List.of();
            }

            String prompt = buildPrompt(branchId, date, periods, auditoriums);

            // ✅ Body đúng format Gemini
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(API_BASE_URL, entity, String.class);

            // ✅ Parse phản hồi
            String content = extractJsonFromGemini(response.getBody());
            if (content == null || content.isBlank()) {
                log.warn("⚠️ Gemini không trả về nội dung hợp lệ.");
                return List.of();
            }

            JsonNode arr = mapper.readTree(content);
            List<Map<String, Object>> list = new ArrayList<>();
            arr.forEach(node -> list.add(mapper.convertValue(node, Map.class)));

            log.info("✅ Gemini trả về {} lịch chiếu.", list.size());
            return list;

        } catch (Exception e) {
            log.error("❌ [AI Scheduler] Lỗi gọi Gemini: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildPrompt(Integer branchId, LocalDate date,
                               List<ScreeningPeriod> periods, List<Auditorium> auditoriums) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hãy tạo lịch chiếu phim cho rạp CineMaster (BranchID=")
                .append(branchId).append(") vào ngày ").append(date).append(".\n")
                .append("Giờ chiếu từ 09:00 đến 23:00. Mỗi phòng tối đa 5 suất.\n")
                .append("Cách nhau ít nhất 15 phút, và không trùng khung giờ.\n\n");

        sb.append("🎞️ SCREENING PERIODS:\n");
        for (ScreeningPeriod p : periods) {
            Movie m = p.getMovie();
            sb.append(String.format("- PeriodID=%d, MovieID=%d, Title='%s', Duration=%d phút [%s → %s]\n",
                    p.getId(), m.getMovieID(), m.getTitle(),
                    m.getDuration(), p.getStartDate(), p.getEndDate()));
        }

        sb.append("\n🏟️ AUDITORIUMS:\n");
        for (Auditorium a : auditoriums) {
            sb.append(String.format("- AuditoriumID=%d, Name='%s', Capacity=%d\n",
                    a.getAuditoriumID(), a.getName(),
                    Optional.ofNullable(a.getCapacity()).orElse(100)));
        }

        sb.append("""
                
        Trả về JSON hợp lệ dạng:
        [
          {
            "movieId": 3,
            "periodId": 7,
            "auditoriumId": 1,
            "startTime": "2025-11-05T09:00",
            "endTime": "2025-11-05T11:10"
          }
        ]
        Không viết thêm giải thích hoặc văn bản ngoài JSON.
        """);

        return sb.toString();
    }

    private String extractJsonFromGemini(String responseBody) {
        try {
            JsonNode node = mapper.readTree(responseBody);
            String text = node
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
            int start = text.indexOf('[');
            int end = text.lastIndexOf(']') + 1;
            return (start != -1 && end > start) ? text.substring(start, end) : null;
        } catch (Exception e) {
            log.error("❌ Không đọc được phản hồi Gemini: {}", e.getMessage());
            return null;
        }
    }
}
