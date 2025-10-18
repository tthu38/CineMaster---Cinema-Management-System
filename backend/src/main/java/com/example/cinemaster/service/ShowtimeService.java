package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.ShowtimeCreateRequest;
import com.example.cinemaster.dto.request.ShowtimeUpdateRequest;
import com.example.cinemaster.dto.response.DayScheduleResponse;
import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.entity.ScreeningPeriod;
import com.example.cinemaster.entity.Seat;
import com.example.cinemaster.entity.Showtime;
import com.example.cinemaster.mapper.ShowtimeMapper;
import com.example.cinemaster.repository.AuditoriumRepository;
import com.example.cinemaster.repository.ScreeningPeriodRepository;
import com.example.cinemaster.repository.SeatRepository;
import com.example.cinemaster.repository.ShowtimeRepository;
import com.example.cinemaster.security.AccountPrincipal;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepo;
    private final ScreeningPeriodRepository periodRepo;
    private final AuditoriumRepository auditoriumRepo;
    private final SeatRepository seatRepository;
    private final ShowtimeMapper mapper;


    private static final int CLEANUP_MINUTES = 15;

    /* ============================================================
       🟦 LẤY CHI TIẾT / TÌM KIẾM SUẤT CHIẾU
    ============================================================ */

    public ShowtimeResponse getById(Integer id) {
        var s = showtimeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));
        return mapper.toResponse(s);
    }

    public Page<ShowtimeResponse> search(Integer periodId, Integer auditoriumId,
                                         LocalDateTime from, LocalDateTime to,
                                         Pageable pageable) {

        Specification<Showtime> spec = (root, query, cb) -> cb.conjunction();

        if (periodId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("period").get("periodID"), periodId));

        if (auditoriumId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("auditorium").get("auditoriumID"), auditoriumId));

        if (from != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("startTime"), from));

        if (to != null)
            spec = spec.and((r, q, cb) -> cb.lessThan(r.get("startTime"), to));

        return showtimeRepo.findAll(spec, pageable).map(mapper::toResponse);
    }

    /* ============================================================
       🟩 TẠO / CẬP NHẬT / XOÁ
    ============================================================ */

    @Transactional
    public ShowtimeResponse create(ShowtimeCreateRequest req, AccountPrincipal user) {
        var period = periodRepo.findById(req.periodId())
                .orElseThrow(() -> new EntityNotFoundException("ScreeningPeriod not found"));
        var auditorium = auditoriumRepo.findById(req.auditoriumId())
                .orElseThrow(() -> new EntityNotFoundException("Auditorium not found"));

        LocalDateTime start = req.startTime();
        LocalDateTime end = req.endTime();

        // ✅ Nếu end nhỏ hơn start (qua 0h) thì cộng thêm 1 ngày
        if (end.isBefore(start)) {
            end = end.plusDays(1);
            System.out.println("⏩ Auto-adjust endTime sang ngày hôm sau: " + end);
        }

        validateShowtime(start, end, period, auditorium, null);

        var entity = mapper.toEntity(req, period, auditorium);
        entity.setStartTime(start);
        entity.setEndTime(end);

        showtimeRepo.saveAndFlush(entity);
        return mapper.toResponse(entity);
    }


    @Transactional
    public ShowtimeResponse update(Integer id, ShowtimeUpdateRequest req, AccountPrincipal user) {
        var entity = showtimeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        var period = periodRepo.findById(req.periodId())
                .orElseThrow(() -> new EntityNotFoundException("ScreeningPeriod not found"));
        var auditorium = auditoriumRepo.findById(req.auditoriumId())
                .orElseThrow(() -> new EntityNotFoundException("Auditorium not found"));

        // 🔒 Manager chỉ được cập nhật trong chi nhánh của mình
        if (user != null && user.isManager()) {
            Integer managerBranch = user.getBranchId();
            Integer auditoriumBranch = auditorium.getBranch().getId();

            if (managerBranch == null || !Objects.equals(managerBranch, auditoriumBranch)) {
                throw new SecurityException("Manager không thể cập nhật showtime ngoài chi nhánh của mình");
            }
        }

        // 🕐 Xử lý thời gian bắt đầu / kết thúc
        LocalDateTime start = req.startTime();
        LocalDateTime end = req.endTime();

        // ✅ Nếu giờ kết thúc nhỏ hơn giờ bắt đầu → qua ngày hôm sau
        if (end.isBefore(start)) {
            end = end.plusDays(1);
            System.out.println("⏩ Auto-adjust endTime sang ngày hôm sau: " + end);
        }

        // ✅ Kiểm tra trùng giờ, giới hạn ngày chiếu, branch, v.v.
        validateShowtime(start, end, period, auditorium, id);

        // ✅ Cập nhật entity
        mapper.updateEntityFromRequest(req, entity, period, auditorium);
        entity.setStartTime(start);
        entity.setEndTime(end);

        showtimeRepo.saveAndFlush(entity);
        return mapper.toResponse(entity);
    }


    @Transactional
    public void delete(Integer id, AccountPrincipal user) {
        var entity = showtimeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        if (user != null && user.isManager()) {
            Integer managerBranch = user.getBranchId();
            Integer showtimeBranch = entity.getAuditorium().getBranch().getId();
            if (managerBranch == null || !Objects.equals(managerBranch, showtimeBranch)) {
                throw new SecurityException("Manager không thể xóa showtime của chi nhánh khác");
            }
        }

        showtimeRepo.delete(entity);
    }

    /* ============================================================
       🧩 VALIDATION
    ============================================================ */

    private void validateShowtime(LocalDateTime start, LocalDateTime end,
                                  ScreeningPeriod period, Auditorium auditorium,
                                  Integer excludeId) {

        System.out.println("\n========== VALIDATE SHOWTIME ==========");
        System.out.println("🎬 Movie: " + period.getMovie().getTitle());
        System.out.println("🏛️ Auditorium: " + auditorium.getName());
        System.out.println("Start: " + start);
        System.out.println("End:   " + end);

        // 🕛 Nếu endTime nhỏ hơn startTime => qua ngày hôm sau
        if (end.isBefore(start)) {
            end = end.plusDays(1);
            System.out.println("⏩ EndTime nhỏ hơn StartTime → tự động +1 ngày");
        }

        // 🏢 Kiểm tra chi nhánh đồng bộ
        if (!Objects.equals(period.getBranch().getId(), auditorium.getBranch().getId())) {
            throw new IllegalArgumentException("❌ Auditorium không thuộc cùng chi nhánh với ScreeningPeriod");
        }

        // 🕐 End phải sau Start
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("❌ EndTime phải lớn hơn StartTime");
        }

        // 📅 Kiểm tra ngày chiếu nằm trong khoảng chiếu
        LocalDate startDate = period.getStartDate();
        LocalDate endDate = period.getEndDate();
        LocalDate showDate = start.toLocalDate();

        if (showDate.isBefore(startDate) || showDate.isAfter(endDate)) {
            throw new IllegalArgumentException(String.format(
                    "❌ Ngày chiếu (%s) phải nằm trong khoảng chiếu của phim: %s → %s",
                    showDate, startDate, endDate
            ));
        }

        // 🕓 Nếu phim chiếu qua 0h (ví dụ 23:30 → 01:45), thì phần kết thúc phải <= endDate + 1 ngày
        if (end.toLocalDate().isAfter(endDate.plusDays(1))) {
            throw new IllegalArgumentException(String.format(
                    "❌ Suất chiếu vượt quá giới hạn cuối của khoảng chiếu (%s → %s)",
                    startDate, endDate
            ));
        }

        // 🧹 Thêm buffer 15 phút để tránh chồng suất
        LocalDateTime startBuf = start.minusMinutes(CLEANUP_MINUTES);
        LocalDateTime endBuf = end.plusMinutes(CLEANUP_MINUTES);

        // 🔍 Kiểm tra trùng khung giờ trong cùng phòng
        long roomClash = (excludeId == null)
                ? showtimeRepo.countOverlaps(auditorium.getAuditoriumID(), startBuf, endBuf)
                : showtimeRepo.countOverlapsExcluding(auditorium.getAuditoriumID(), startBuf, endBuf, excludeId);

        if (roomClash > 0) {
            throw new IllegalStateException("❌ Khung giờ vi phạm khoảng đệm 15 phút trong cùng phòng chiếu");
        }

        // 🎞️ Kiểm tra cùng phim trùng giờ trong cùng phòng/chi nhánh
        var movieId = period.getMovie().getMovieID();
        var branchId = auditorium.getBranch().getId();
        var auditoriumId = auditorium.getAuditoriumID();

        long movieClash = (excludeId == null)
                ? showtimeRepo.countMovieOverlapInBranch(movieId, branchId, auditoriumId, start, end)
                : showtimeRepo.countMovieOverlapInBranchExcluding(movieId, branchId, auditoriumId, start, end, excludeId);

        if (movieClash > 0) {
            throw new IllegalStateException("❌ Phim này đã có suất chiếu khác trong cùng phòng ở khung giờ trùng");
        }

        System.out.println("✅ Showtime hợp lệ!");
    }


    /* ============================================================
       📅 LỊCH CHIẾU THEO TUẦN
    ============================================================ */

    public List<DayScheduleResponse> getNextWeekSchedule(Integer branchId) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
        return buildWeek(nextMonday, branchId);
    }

    public List<DayScheduleResponse> getWeekSchedule(LocalDate anchor, Integer branchId) {
        LocalDate base = (anchor != null) ? anchor : LocalDate.now();
        LocalDate monday = base.minusDays((base.getDayOfWeek().getValue() + 6) % 7);
        return buildWeek(monday, branchId);
    }

    /* ============================================================
       🧮 BUILD WEEK LOGIC
    ============================================================ */

    private List<DayScheduleResponse> buildWeek(LocalDate monday, Integer branchId) {
        LocalDate sunday = monday.plusDays(7);
        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to = sunday.atStartOfDay();

        List<Showtime> list = (branchId == null)
                ? showtimeRepo.findAllByStartTimeGreaterThanEqualAndStartTimeLessThan(from, to)
                : showtimeRepo.findWeekByBranch(from, to, branchId);

        Map<LocalDate, Map<Integer, List<Showtime>>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStartTime().toLocalDate(),
                        Collectors.groupingBy(s -> s.getPeriod().getMovie().getMovieID())
                ));

        List<DayScheduleResponse> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            Map<Integer, List<Showtime>> byMovie = grouped.getOrDefault(d, Collections.emptyMap());

            List<DayScheduleResponse.MovieSlots> movies = byMovie.entrySet().stream().map(e -> {
                Integer movieId = e.getKey();
                List<Showtime> slots = e.getValue().stream()
                        .sorted(Comparator.comparing(Showtime::getStartTime))
                        .toList();

                String title = slots.get(0).getPeriod().getMovie().getTitle();
                String poster = slots.get(0).getPeriod().getMovie().getPosterUrl();

                List<DayScheduleResponse.SlotItem> slotItems = slots.stream().map(s -> {
                    var auditorium = s.getAuditorium();
                    int auditoriumId = auditorium.getAuditoriumID();

                    // ✅ Lấy danh sách ghế an toàn
                    List<Seat> seats = seatRepository.findByAuditorium_AuditoriumID(auditoriumId);

                    int totalSeats = seats.size();
                    long availableSeats = seats.stream()
                            .filter(seat -> seat.getStatus() != null && seat.getStatus().equals(Seat.SeatStatus.AVAILABLE))
                            .count();

                    return new DayScheduleResponse.SlotItem(
                            s.getShowtimeID(),
                            auditoriumId,
                            auditorium.getName(),
                            s.getStartTime(),
                            s.getEndTime(),
                            (int) availableSeats,
                            totalSeats
                    );
                }).toList();



                return new DayScheduleResponse.MovieSlots(movieId, title, poster, slotItems);
            }).sorted(Comparator.comparing(DayScheduleResponse.MovieSlots::movieTitle)).toList();

            days.add(new DayScheduleResponse(d, movies));
        }
        return days;
    }
}
