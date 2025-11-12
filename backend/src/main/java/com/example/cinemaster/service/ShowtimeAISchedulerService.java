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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeAISchedulerService {

    // 🔑 API Keys
    @Value("${gemini.api.keyShowTime}")
    private String primaryKey;

    @Value("${gemini.api.keyShowTime.backup:}")
    private String backupKey; // optional key dự phòng

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final ScreeningPeriodRepository periodRepo;
    private final AuditoriumRepository auditoriumRepo;
    private final ShowtimeRepository showtimeRepo;
    private final TicketRepository ticketRepo;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     *  ✅ Sinh lịch chiếu tự động bằng AI, có retry + fallback + auto key switch
     */
    public List<Map<String, Object>> generateSchedule(Integer branchId, LocalDate date) {
        String geminiKey = primaryKey;
        String modelMain = "gemini-2.5-flash";
        String modelBackup = "gemini-1.5-pro-latest";

        try {
            // --- Chuẩn bị dữ liệu ---
            List<ScreeningPeriod> periods = periodRepo.findActive(branchId, date);
            List<Auditorium> auditoriums = auditoriumRepo.findActiveByBranch(branchId);
            List<Showtime> existing = showtimeRepo.findByBranchIdAndDate(branchId, date);

            if (periods.isEmpty() || auditoriums.isEmpty()) {
                log.warn(" Không có Period hoặc Auditorium hợp lệ để tạo lịch chiếu.");
                return List.of();
            }

            // --- Xác định phim hot tuần qua ---
            ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDateTime from = LocalDate.now(zone).minusDays(7).atStartOfDay();
            LocalDateTime to = LocalDateTime.now(zone);
            List<Object[]> topMovies = ticketRepo.findTop10MoviesByTickets(branchId, from, to);

            Set<String> hotMovieTitles = topMovies.stream()
                    .map(obj -> (String) obj[0])
                    .limit(3)
                    .collect(Collectors.toSet());

            log.info(" Top phim hot tuần qua: {}", hotMovieTitles);

            String prompt = buildPrompt(branchId, date, periods, auditoriums, existing, hotMovieTitles);

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(Map.of("text", prompt))
                    ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // --- Thử gửi đến Gemini (retry 5 lần + fallback) ---
            ResponseEntity<String> response = null;
            boolean success = false;

            for (int attempt = 1; attempt <= 5; attempt++) {
                String apiUrl = geminiApiUrl + "/" + modelMain + ":generateContent?key=" + geminiKey;

                try {
                    response = restTemplate.postForEntity(apiUrl, entity, String.class);
                    success = true;
                    break;

                } catch (HttpServerErrorException e) {
                    // 503 - Model quá tải
                    if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && attempt < 5) {
                        log.warn("⚠️ Gemini quá tải (503), thử lại lần {}/5 sau {}s...", attempt, attempt * 3);
                        Thread.sleep(attempt * 3000L);
                        continue;

                    } else if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && backupKey != null && !backupKey.isBlank()) {
                        // nếu vẫn lỗi => thử key backup
                        log.warn("🔑 Key chính bị nghẽn, chuyển sang key dự phòng...");
                        geminiKey = backupKey;
                        headers.set("x-goog-api-key", geminiKey);
                        entity = new HttpEntity<>(body, headers);
                        continue;

                    } else {
                        // fallback model
                        log.warn("🔁 Chuyển sang model dự phòng: {}", modelBackup);
                        String fallbackUrl = geminiApiUrl + "/" + modelBackup + ":generateContent?key=" + geminiKey;
                        response = restTemplate.postForEntity(fallbackUrl, entity, String.class);
                        success = true;
                        break;
                    }
                }
            }

            if (!success || response == null) {
                log.error("❌ AI không phản hồi sau nhiều lần thử.");
                return List.of();
            }

            // --- Parse JSON trả về ---
            String content = extractJsonFromGemini(response.getBody());
            if (content == null || content.isBlank()) {
                log.warn("⚠️ Gemini không trả về JSON hợp lệ.");
                return List.of();
            }

            JsonNode arr = mapper.readTree(content);
            List<Map<String, Object>> list = new ArrayList<>();
            arr.forEach(node -> list.add(mapper.convertValue(node, Map.class)));

            log.info("✅ Gemini trả về {} suất chiếu mới.", list.size());
            return list;

        } catch (Exception e) {
            log.error("❌ [AI Scheduler] Lỗi khi tạo lịch chiếu: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ====================== PROMPT BUILDER ======================
    private String buildPrompt(Integer branchId, LocalDate date,
                               List<ScreeningPeriod> periods,
                               List<Auditorium> auditoriums,
                               List<Showtime> existing,
                               Set<String> hotMovieTitles) {

        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isHoliday = isHoliday(date);

        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là hệ thống lập lịch chiếu phim thông minh cho rạp CineMaster (BranchID=")
                .append(branchId).append(") vào ngày ").append(date).append(".\n")
                .append("Giờ chiếu: 09:00 - 23:00. Mỗi phòng tối đa 5 suất.\n")
                .append("Các phòng chiếu độc lập, không trùng suất trong cùng phòng, và 2 suất liền kề cách nhau tối thiểu 15 phút.\n");

        if (isHoliday) {
            sb.append("⚡ Đây là ngày lễ, tăng giá vé thêm 15000đ.\n");
        } else if (isWeekend) {
            sb.append("🎉 Cuối tuần, tăng giá vé thêm 15000đ.\n");
        }

        sb.append("Phim HOT (bán được nhiều vé nhất tuần qua) được ưu tiên chiếu nhiều hơn và cộng thêm 15000đ.\n\n");

        sb.append("SCREENING PERIODS:\n");
        for (ScreeningPeriod p : periods) {
            Movie m = p.getMovie();
            boolean isHot = hotMovieTitles.contains(m.getTitle());
            sb.append(String.format(
                    "- PeriodID=%d, MovieID=%d, Title='%s', Duration=%d phút, Hot=%s [%s → %s]\n",
                    p.getId(), m.getMovieID(), m.getTitle(),
                    m.getDuration(), isHot ? "Yes" : "No",
                    p.getStartDate(), p.getEndDate()));
        }

        sb.append("\nAUDITORIUMS:\n");
        for (Auditorium a : auditoriums) {
            sb.append(String.format("- AuditoriumID=%d, Name='%s', Capacity=%d\n",
                    a.getAuditoriumID(), a.getName(),
                    Optional.ofNullable(a.getCapacity()).orElse(100)));
        }

        sb.append("\nSUẤT CHIẾU ĐÃ CÓ TRONG NGÀY:\n");
        for (Showtime s : existing) {
            sb.append(String.format("- %s: %s → %s (%s)\n",
                    s.getAuditorium().getName(),
                    s.getStartTime(), s.getEndTime(),
                    s.getPeriod().getMovie().getTitle()));
        }

        sb.append("""
Trả về JSON hợp lệ dạng:
[
  {
    "movieId": 3,
    "periodId": 7,
    "auditoriumId": 1,
    "language": "Vietnamese",
    "startTime": "2025-11-05T09:00",
    "endTime": "2025-11-05T11:10",
    "price": 120000
  }
]

⚠️ QUY TẮC BẮT BUỘC:
- Không trùng hoặc chồng chéo với bất kỳ suất chiếu nào đã liệt kê ở phần trên ("SUẤT CHIẾU ĐÃ CÓ TRONG NGÀY").
- Mỗi phòng chiếu tối đa 5 suất, hai suất cùng phòng phải cách nhau ít nhất 15 phút.
- Mỗi phim cần ít nhất 1 suất "Vietnamese" và 1 suất "English".
- Ưu tiên chiếu nhiều hơn cho các phim HOT, cuối tuần hoặc lễ (tăng giá +15000₫).
- Thời gian chiếu trong khung 09:00 → 23:00.
- Chỉ trả về JSON thuần, không giải thích, không ghi chú, không Markdown.
""");

        return sb.toString();
    }

    // ====================== PARSER ======================
    private String extractJsonFromGemini(String responseBody) {
        try {
            JsonNode node = mapper.readTree(responseBody);
            String text = node.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
            int start = text.indexOf('[');
            int end = text.lastIndexOf(']') + 1;
            return (start != -1 && end > start) ? text.substring(start, end) : null;
        } catch (Exception e) {
            log.error("⚠️ Không đọc được phản hồi Gemini: {}", e.getMessage());
            return null;
        }
    }

    // ====================== UTILITIES ======================
    public BigDecimal calculatePrice(BigDecimal basePrice, boolean isWeekend, boolean isHoliday, boolean isHot) {
        BigDecimal result = basePrice;
        if (isWeekend || isHoliday || isHot) {
            result = result.add(BigDecimal.valueOf(15000));
        }
        return result;
    }

    public boolean isHoliday(LocalDate date) {
        int day = date.getDayOfMonth();
        int month = date.getMonthValue();
        Set<String> holidays = Set.of("1-1", "30-4", "1-5", "2-9", "25-12");
        String key = day + "-" + month;
        if (holidays.contains(key)) return true;

        LocalDate tetStart = LocalDate.of(date.getYear(), 1, 28);
        LocalDate tetEnd = LocalDate.of(date.getYear(), 2, 3);
        return !date.isBefore(tetStart) && !date.isAfter(tetEnd);
    }
}
