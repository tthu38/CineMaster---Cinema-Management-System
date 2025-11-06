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

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShowtimeAISchedulerService {

    @Value("${gemini.api.keyShowTime}")
    private String geminiApiKey;

    private final ScreeningPeriodRepository periodRepo;
    private final AuditoriumRepository auditoriumRepo;
    private final ShowtimeRepository showtimeRepo;
    private final TicketRepository ticketRepo;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String API_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    /**
     * 🧠 Sinh lịch chiếu bằng AI, tránh trùng suất, tự động tính phim hot & giá vé.
     */
    public List<Map<String, Object>> generateSchedule(Integer branchId, LocalDate date) {
        try {
            // 🔹 Lấy dữ liệu đầu vào
            List<ScreeningPeriod> periods = periodRepo.findActive(branchId, date);
            List<Auditorium> auditoriums = auditoriumRepo.findActiveByBranch(branchId);
            List<Showtime> existing = showtimeRepo.findByBranchIdAndDate(branchId, date);

            if (periods.isEmpty() || auditoriums.isEmpty()) {
                log.warn("⚠️ Không có dữ liệu Period hoặc Auditorium hợp lệ để tạo lịch.");
                return List.of();
            }

            // 🔥 Xác định phim hot dựa theo số vé BOOKED/USED trong 7 ngày gần đây
            LocalDateTime from = date.minusDays(7).atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay();
            List<Object[]> topMovies = ticketRepo.findTop10MoviesByTickets(branchId, from, to);

            Set<String> hotMovieTitles = topMovies.stream()
                    .map(obj -> (String) obj[0])  // obj[0] là m.title
                    .limit(3)
                    .collect(Collectors.toSet());

            log.info("🔥 Top phim hot tuần qua: {}", hotMovieTitles);

            // 🧩 Tạo prompt chi tiết
            String prompt = buildPrompt(branchId, date, periods, auditoriums, existing, hotMovieTitles);

            // ✅ Gọi Gemini API
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response =
                    restTemplate.postForEntity(API_BASE_URL, entity, String.class);

            // ✅ Parse phản hồi từ Gemini
            String content = extractJsonFromGemini(response.getBody());
            if (content == null || content.isBlank()) {
                log.warn("⚠️ Gemini không trả về nội dung hợp lệ.");
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

    /**
     * 🏗️ Xây dựng prompt đầy đủ để gửi cho Gemini.
     */
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
                .append("Mỗi phòng chiếu là độc lập, vì vậy nhiều phim có thể chiếu cùng lúc ở các phòng khác nhau. "
                        + "Chỉ cần đảm bảo một phòng không bị trùng suất chiếu và 2 suất chiếu liên tiếp trong cùng 1 phòng cách nhau 15 phút.\n")
        ;

        if (isHoliday) {
            sb.append("⚡ Đây là ngày lễ, hãy ưu tiên các phim nổi bật và cộng thêm 15000đ giá vé.\n");
        } else if (isWeekend) {
            sb.append("📅 Đây là cuối tuần, tăng giá vé +15000đ.\n");
        }

        sb.append("Phim HOT (bán được nhiều vé nhất tuần qua) được ưu tiên chiếu nhiều hơn và giá vé +15000đ.\n\n");

        // 🎬 DANH SÁCH PHIM
        sb.append("🎞️ SCREENING PERIODS:\n");
        for (ScreeningPeriod p : periods) {
            Movie m = p.getMovie();
            boolean isHot = hotMovieTitles.contains(m.getTitle());
            sb.append(String.format(
                    "- PeriodID=%d, MovieID=%d, Title='%s', Duration=%d phút, Hot=%s [%s → %s]\n",
                    p.getId(), m.getMovieID(), m.getTitle(),
                    m.getDuration(), isHot ? "Yes" : "No",
                    p.getStartDate(), p.getEndDate()));
        }

        // 🏟️ DANH SÁCH PHÒNG
        sb.append("\n🏟️ AUDITORIUMS:\n");
        for (Auditorium a : auditoriums) {
            sb.append(String.format("- AuditoriumID=%d, Name='%s', Capacity=%d\n",
                    a.getAuditoriumID(), a.getName(),
                    Optional.ofNullable(a.getCapacity()).orElse(100)));
        }

        // 🕒 SUẤT CHIẾU HIỆN CÓ
        sb.append("\n🕒 SUẤT CHIẾU ĐÃ CÓ TRONG NGÀY:\n");
        for (Showtime s : existing) {
            sb.append(String.format("- %s: %s → %s (%s)\n",
                    s.getAuditorium().getName(),
                    s.getStartTime(), s.getEndTime(),
                    s.getPeriod().getMovie().getTitle()));
        }

        // 💬 YÊU CẦU JSON
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
Quy tắc:
- Mỗi phim phải có ít nhất 1 suất chiếu bằng 'Vietnamese' và 1 suất chiếu bằng 'English' trong ngày.
- Các suất chiếu cách nhau ít nhất 15 phút, không trùng giờ.
- Phim HOT hoặc ngày cuối tuần/lễ → cộng thêm 15000đ.
- Không viết thêm văn bản ngoài JSON.
""");


        return sb.toString();
    }

    /**
     * 📤 Tách phần JSON trả về từ phản hồi Gemini
     */
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

    /**
     * 💰 Tính giá vé (áp dụng logic tăng giá)
     */
    public BigDecimal calculatePrice(BigDecimal basePrice, boolean isWeekend, boolean isHoliday, boolean isHot) {
        BigDecimal result = basePrice;
        if (isWeekend || isHoliday || isHot) {
            result = result.add(BigDecimal.valueOf(15000));
        }
        return result;
    }

    /**
     * 🎉 Kiểm tra ngày lễ (theo dương lịch Việt Nam)
     */
    public boolean isHoliday(LocalDate date) {
        int day = date.getDayOfMonth();
        int month = date.getMonthValue();

        // 🎆 Danh sách các ngày lễ cố định
        Set<String> holidays = Set.of(
                "1-1",   // Tết Dương lịch
                "30-4",  // Giải phóng miền Nam
                "1-5",   // Quốc tế Lao động
                "2-9",   // Quốc khánh
                "25-12"  // Giáng Sinh
        );

        String key = day + "-" + month;
        if (holidays.contains(key)) return true;

        // 🧧 Khoảng thời gian Tết Nguyên Đán (ước lượng dương lịch)
        LocalDate tetStart = LocalDate.of(date.getYear(), 1, 28);
        LocalDate tetEnd = LocalDate.of(date.getYear(), 2, 3);
        if (!date.isBefore(tetStart) && !date.isAfter(tetEnd)) return true;

        return false;
    }
}
