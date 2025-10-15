package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.ShowtimeCreateRequest;
import com.example.cinemaster.dto.request.ShowtimeUpdateRequest;
import com.example.cinemaster.dto.response.DayScheduleResponse;
import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.entity.Showtime;
import com.example.cinemaster.mapper.ShowtimeMapper;
import com.example.cinemaster.repository.AuditoriumRepository;
import com.example.cinemaster.repository.ScreeningPeriodRepository;
import com.example.cinemaster.repository.ShowtimeRepository;
import com.example.cinemaster.security.AccountPrincipal;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepo;
    private final ScreeningPeriodRepository periodRepo;
    private final AuditoriumRepository auditoriumRepo;
    private final ShowtimeMapper mapper;

    private static final int CLEANUP_MINUTES = 15;

    public ShowtimeResponse getById(Integer id) {
        var s = showtimeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));
        return mapper.toResponse(s);
    }

    // SEARCH
    public Page<ShowtimeResponse> search(Integer periodId, Integer auditoriumId,
                                         LocalDateTime from, LocalDateTime to,
                                         Pageable pageable) {

        // ✅ Dùng conjunction để tránh null specification
        Specification<Showtime> spec = (root, query, cb) -> cb.conjunction();

        if (periodId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("period").get("id"), periodId));

        if (auditoriumId != null)
            spec = spec.and((r, q, cb) -> cb.equal(r.get("auditorium").get("auditoriumID"), auditoriumId));

        if (from != null)
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("startTime"), from));

        if (to != null)
            spec = spec.and((r, q, cb) -> cb.lessThan(r.get("startTime"), to));

        return showtimeRepo.findAll(spec, pageable)
                .map(mapper::toResponse);
    }

    @Transactional
    public ShowtimeResponse create(ShowtimeCreateRequest req, AccountPrincipal user) {
        var period = periodRepo.findById(req.periodId())
                .orElseThrow(() -> new EntityNotFoundException("ScreeningPeriod not found"));
        var auditorium = auditoriumRepo.findById(req.auditoriumId())
                .orElseThrow(() -> new EntityNotFoundException("Auditorium not found"));

        // 🔒 Chỉ Manager bị giới hạn chi nhánh
        if (user != null && user.isManager()) {
            if (!Objects.equals(auditorium.getBranch().getId(), user.getBranchId())) {
                throw new SecurityException("Manager không thể tạo showtime cho chi nhánh khác");
            }
        }

        validateShowtime(req.startTime(), req.endTime(), period, auditorium, null);

        var entity = mapper.toEntity(req, period, auditorium);
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

        // 🔒 Manager chỉ được sửa trong chi nhánh của mình
        if (user != null && user.isManager()) {
            if (!Objects.equals(auditorium.getBranch().getId(), user.getBranchId())) {
                throw new SecurityException("Manager không thể cập nhật showtime ngoài chi nhánh của mình");
            }
        }

        validateShowtime(req.startTime(), req.endTime(), period, auditorium, id);

        mapper.updateEntityFromRequest(req, entity, period, auditorium);
        showtimeRepo.save(entity);
        return mapper.toResponse(entity);
    }



    @Transactional
    public void delete(Integer id, AccountPrincipal user) {
        var entity = showtimeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        // 🔒 Manager chỉ được xóa trong chi nhánh của mình
        if (user != null && user.isManager()) {
            Integer branchOfShowtime = entity.getAuditorium().getBranch().getId();
            if (!Objects.equals(branchOfShowtime, user.getBranchId())) {
                throw new SecurityException("Manager không thể xóa showtime của chi nhánh khác");
            }
        }

        showtimeRepo.delete(entity);
    }



    // ========= VALIDATION LOGIC =========
    private void validateShowtime(LocalDateTime start, LocalDateTime end,
                                  com.example.cinemaster.entity.ScreeningPeriod period,
                                  com.example.cinemaster.entity.Auditorium auditorium,
                                  Integer excludeId) {

        if (!Objects.equals(period.getBranch().getId(), auditorium.getBranch().getId()))
            throw new IllegalArgumentException("Auditorium không thuộc cùng chi nhánh với ScreeningPeriod");

        if (!end.isAfter(start))
            throw new IllegalArgumentException("EndTime phải lớn hơn StartTime");

        if (start.toLocalDate().isBefore(period.getStartDate())
                || end.toLocalDate().isAfter(period.getEndDate()))
            throw new IllegalArgumentException("Suất chiếu phải nằm trong khoảng ScreeningPeriod");

        // Overlap trong cùng phòng (buffer 15’)
        var startBuf = start.minusMinutes(CLEANUP_MINUTES);
        var endBuf = end.plusMinutes(CLEANUP_MINUTES);

        long roomClash = (excludeId == null)
                ? showtimeRepo.countOverlaps(auditorium.getAuditoriumID(), startBuf, endBuf)
                : showtimeRepo.countOverlapsExcluding(auditorium.getAuditoriumID(), startBuf, endBuf, excludeId);

        if (roomClash > 0)
            throw new IllegalStateException("Khung giờ vi phạm khoảng đệm 15 phút của auditorium này");

        // Cùng phim, cùng branch, cùng khung giờ
        var movieId = period.getMovie().getMovieID();
        var branchId = auditorium.getBranch().getId();
        long movieClash = (excludeId == null)
                ? showtimeRepo.countMovieOverlapInBranch(movieId, branchId, start, end)
                : showtimeRepo.countMovieOverlapInBranchExcluding(movieId, branchId, start, end, excludeId);

        if (movieClash > 0)
            throw new IllegalStateException("Phim này đã có suất chiếu khác trong cùng branch ở khung giờ trùng.");
    }

    // ========= LỊCH TUẦN =========

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

    // ========= Helper =========
    private List<DayScheduleResponse> buildWeek(LocalDate monday, Integer branchId) {
        LocalDate sunday = monday.plusDays(7);
        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to = sunday.atStartOfDay();

        List<Showtime> list = (branchId == null)
                ? showtimeRepo.findAllByStartTimeGreaterThanEqualAndStartTimeLessThan(from, to)
                : showtimeRepo.findAllByStartTimeGreaterThanEqualAndStartTimeLessThanAndAuditorium_Branch_Id(from, to, branchId);

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

                List<DayScheduleResponse.SlotItem> slotItems = slots.stream().map(s ->
                        new DayScheduleResponse.SlotItem(
                                s.getShowtimeID(),
                                s.getAuditorium().getAuditoriumID(),
                                s.getAuditorium().getName(),
                                s.getStartTime(),
                                s.getEndTime()
                        )
                ).toList();

                return new DayScheduleResponse.MovieSlots(movieId, title, poster, slotItems);
            }).sorted(Comparator.comparing(DayScheduleResponse.MovieSlots::movieTitle)).toList();

            days.add(new DayScheduleResponse(d, movies));
        }
        return days;
    }
}
