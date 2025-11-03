package com.example.cinemaster.service;


import com.example.cinemaster.dto.request.ShowtimeCreateRequest;
import com.example.cinemaster.dto.request.ShowtimeUpdateRequest;
import com.example.cinemaster.dto.response.DayScheduleResponse;
import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.entity.ScreeningPeriod;
import com.example.cinemaster.entity.Showtime;
import com.example.cinemaster.mapper.ShowtimeMapper;
import com.example.cinemaster.repository.*;
import com.example.cinemaster.security.AccountPrincipal;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


import java.time.*;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
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
       🔹 LẤY CHI TIẾT / TÌM KIẾM SUẤT CHIẾU
    ============================================================ */
    public ShowtimeResponse getById(Integer id) {
        var s = showtimeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));
        return mapper.toResponse(s);
    }


    /* ============================================================
   🔎 TÌM KIẾM SUẤT CHIẾU (lọc & phân trang)
============================================================ */
    public Page<ShowtimeResponse> search(
            Integer periodId,
            Integer auditoriumId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Specification<Showtime> spec = (root, query, cb) -> cb.conjunction();

        // 🔹 Chỉ lấy showtime ACTIVE (không bị xóa mềm)
        spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), "ACTIVE"));

        // 🔹 Lọc theo ScreeningPeriod
        if (periodId != null) {
            spec = spec.and((r, q, cb) ->
                    cb.equal(r.get("period").get("periodID"), periodId));
        }

        // 🔹 Lọc theo phòng chiếu
        if (auditoriumId != null) {
            spec = spec.and((r, q, cb) ->
                    cb.equal(r.get("auditorium").get("auditoriumID"), auditoriumId));
        }

        // 🔹 Lọc theo khoảng thời gian (from → to)
        if (from != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("startTime"), from));
        }
        if (to != null) {
            spec = spec.and((r, q, cb) -> cb.lessThan(r.get("startTime"), to));
        }

        // ✅ Trả về Page<ShowtimeResponse>
        Page<Showtime> result = showtimeRepo.findAll(spec, pageable);
        return result.map(mapper::toResponse);
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


        if (end.isBefore(start)) {
            end = end.plusDays(1);
            log.info("⏩ Auto-adjust endTime sang ngày hôm sau: {}", end);
        }


        validateShowtime(start, end, period, auditorium, null);


        var entity = mapper.toEntity(req, period, auditorium);
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setStatus("ACTIVE");

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


        if (user != null && user.isManager()) {
            Integer managerBranch = user.getBranchId();
            Integer auditoriumBranch = auditorium.getBranch().getId();
            if (managerBranch == null || !Objects.equals(managerBranch, auditoriumBranch)) {
                throw new SecurityException("Manager không thể cập nhật showtime ngoài chi nhánh của mình");
            }
        }


        LocalDateTime start = req.startTime();
        LocalDateTime end = req.endTime();
        if (end.isBefore(start)) end = end.plusDays(1);


        validateShowtime(start, end, period, auditorium, id);


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

        // ✅ Soft delete
        entity.setStatus("INACTIVE");
        showtimeRepo.saveAndFlush(entity);
    }



    /* ============================================================
       🧩 VALIDATION
    ============================================================ */
    private void validateShowtime(LocalDateTime start, LocalDateTime end,
                                  ScreeningPeriod period, Auditorium auditorium,
                                  Integer excludeId) {


        if (!Objects.equals(period.getBranch().getId(), auditorium.getBranch().getId()))
            throw new IllegalArgumentException("❌ Auditorium không thuộc cùng chi nhánh với ScreeningPeriod");


        if (!end.isAfter(start))
            throw new IllegalArgumentException("❌ EndTime phải lớn hơn StartTime");


        LocalDate showDate = start.toLocalDate();
        if (showDate.isBefore(period.getStartDate()) || showDate.isAfter(period.getEndDate()))
            throw new IllegalArgumentException("❌ Ngày chiếu nằm ngoài khoảng chiếu phim");


        if (end.toLocalDate().isAfter(period.getEndDate().plusDays(1)))
            throw new IllegalArgumentException("❌ Suất chiếu vượt quá giới hạn cuối của khoảng chiếu");


        LocalDateTime startBuf = start.minusMinutes(CLEANUP_MINUTES);
        LocalDateTime endBuf = end.plusMinutes(CLEANUP_MINUTES);


        long roomClash = (excludeId == null)
                ? showtimeRepo.countOverlaps(auditorium.getAuditoriumID(), startBuf, endBuf)
                : showtimeRepo.countOverlapsExcluding(auditorium.getAuditoriumID(), startBuf, endBuf, excludeId);
        if (roomClash > 0)
            throw new IllegalStateException("❌ Khung giờ vi phạm khoảng đệm 15 phút trong cùng phòng chiếu");


        long movieClash = (excludeId == null)
                ? showtimeRepo.countMovieOverlapInBranch(period.getMovie().getMovieID(), auditorium.getBranch().getId(),
                auditorium.getAuditoriumID(), start, end)
                : showtimeRepo.countMovieOverlapInBranchExcluding(period.getMovie().getMovieID(),
                auditorium.getBranch().getId(), auditorium.getAuditoriumID(), start, end, excludeId);
        if (movieClash > 0)
            throw new IllegalStateException("❌ Phim này đã có suất chiếu khác trong cùng phòng ở khung giờ trùng");
    }


    /* ============================================================
       📅 LỊCH CHIẾU THEO TUẦN
    ============================================================ */
    public List<DayScheduleResponse> getNextWeekSchedule(Integer branchId) {
        return getWeekSchedule(LocalDate.now().plusWeeks(1), branchId, null);
    }


    public List<DayScheduleResponse> getWeekSchedule(LocalDate anchor, Integer branchId, Integer movieId) {
        LocalDate base = (anchor != null) ? anchor : LocalDate.now();
        LocalDate monday = base.minusDays((base.getDayOfWeek().getValue() + 6) % 7);
        return buildWeek(monday, branchId, movieId);
    }




    /* ============================================================
       🧮 BUILD WEEK LOGIC (ĐÃ FIX)
    ============================================================ */
    private List<DayScheduleResponse> buildWeek(LocalDate monday, Integer branchId, Integer movieId) {
        LocalDate sunday = monday.plusDays(7);
        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to = sunday.atStartOfDay();


        List<Showtime> list;
        try {
            if (branchId == null && movieId == null) {
                list = showtimeRepo.findAllByStartTimeGreaterThanEqualAndStartTimeLessThanAndStatus(from, to, "ACTIVE");
            } else if (branchId != null && movieId == null) {
                list = showtimeRepo.findWeekByBranch(from, to, branchId);
            } else if (branchId != null) {
                list = showtimeRepo.findByBranchAndMovieInRange(branchId, movieId, from, to);
            } else {
                list = showtimeRepo.findByMovieInRange(movieId, from, to);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi truy vấn showtime tuần: {}", e.getMessage());
            return Collections.emptyList();
        }


        // phần còn lại giữ nguyên như buildWeek cũ
        Map<LocalDate, Map<Integer, List<Showtime>>> grouped = list.stream()
                .filter(s -> s.getStartTime() != null && s.getPeriod() != null && s.getPeriod().getMovie() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getStartTime().toLocalDate(),
                        Collectors.groupingBy(s -> s.getPeriod().getMovie().getMovieID())
                ));


        List<DayScheduleResponse> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            Map<Integer, List<Showtime>> byMovie = grouped.getOrDefault(d, Collections.emptyMap());


            List<DayScheduleResponse.MovieSlots> movies = byMovie.entrySet().stream()
                    .map(e -> {
                        Integer movieIdKey = e.getKey();
                        List<Showtime> slots = e.getValue().stream()
                                .sorted(Comparator.comparing(Showtime::getStartTime))
                                .toList();
                        if (slots.isEmpty()) return null;


                        String title = Optional.ofNullable(slots.get(0).getPeriod().getMovie().getTitle()).orElse("(Không tên)");
                        String poster = Optional.ofNullable(slots.get(0).getPeriod().getMovie().getPosterUrl()).orElse("/uploads/no-poster.png");


                        List<DayScheduleResponse.SlotItem> slotItems = slots.stream().map(s -> {
                            var auditorium = s.getAuditorium();
                            return new DayScheduleResponse.SlotItem(
                                    s.getShowtimeID(),
                                    auditorium.getAuditoriumID(),
                                    auditorium.getName(),
                                    s.getStartTime(),
                                    s.getEndTime()
                            );
                        }).toList();


                        return new DayScheduleResponse.MovieSlots(movieIdKey, title, poster, slotItems);
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(DayScheduleResponse.MovieSlots::movieTitle))
                    .toList();


            days.add(new DayScheduleResponse(d, movies));
        }
        return days;
    }




    // =================== BỔ TRỢ ===================
    public List<Showtime> getShowtimesByBranchAndDate(Integer branchId, LocalDate date) {
        if (date == null) date = LocalDate.now();
        return showtimeRepo.findByBranchIdAndDate(branchId, date);
    }
}
