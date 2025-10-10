// src/main/java/com/example/cinemaster/service/ShowtimeService.java
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
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepo;
    private final ScreeningPeriodRepository periodRepo;
    private final AuditoriumRepository auditoriumRepo;
    private final ShowtimeMapper mapper;

    private static final int CLEANUP_MINUTES = 15;

    public ShowtimeService(
            ShowtimeRepository showtimeRepo,
            ScreeningPeriodRepository periodRepo,
            AuditoriumRepository auditoriumRepo,
            ShowtimeMapper mapper
    ) {
        this.showtimeRepo = showtimeRepo;
        this.periodRepo = periodRepo;
        this.auditoriumRepo = auditoriumRepo;
        this.mapper = mapper;
    }

    // ========= CRUD =========

    public ShowtimeResponse getById(Integer id) {
        var s = showtimeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));
        return mapper.toResponse(s);
    }

    // SEARCH
    public Page<ShowtimeResponse> search(Integer periodId, Integer auditoriumId,
                                         LocalDateTime from, LocalDateTime to,
                                         Pageable pageable) {
        Specification<Showtime> spec = Specification.where(null);

        if (periodId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("period").get("id"), periodId));
        }
        if (auditoriumId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("auditorium").get("auditoriumID"), auditoriumId));
        }
        if (from != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("startTime"), from));
        }
        if (to != null) {
            spec = spec.and((r, q, cb) -> cb.lessThan(r.get("startTime"), to));
        }

        return showtimeRepo.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public ShowtimeResponse create(ShowtimeCreateRequest req) {
        var period = periodRepo.findById(req.periodId())
                .orElseThrow(() -> new EntityNotFoundException("ScreeningPeriod not found"));
        var auditorium = auditoriumRepo.findById(req.auditoriumId())
                .orElseThrow(() -> new EntityNotFoundException("Auditorium not found"));

        // Cùng chi nhánh
        if (!Objects.equals(period.getBranch().getId(), auditorium.getBranch().getId()))
            throw new IllegalArgumentException("Auditorium không thuộc cùng chi nhánh với ScreeningPeriod");

        // start < end
        if (!req.endTime().isAfter(req.startTime()))
            throw new IllegalArgumentException("EndTime phải lớn hơn StartTime");

        // Nằm trong khoảng period (theo ngày)
        if (req.startTime().toLocalDate().isBefore(period.getStartDate())
                || req.endTime().toLocalDate().isAfter(period.getEndDate()))
            throw new IllegalArgumentException("Suất chiếu phải nằm trong khoảng ScreeningPeriod");

        // 1) Chống trùng giờ trong cùng phòng có buffer 15'
        var startWithBuf = req.startTime().minusMinutes(CLEANUP_MINUTES);
        var endWithBuf   = req.endTime().plusMinutes(CLEANUP_MINUTES);
        var overlaps = showtimeRepo.findOverlapsWithBufferForUpdate(
                req.auditoriumId(), startWithBuf, endWithBuf);
        if (!overlaps.isEmpty())
            throw new IllegalStateException("Khung giờ vi phạm khoảng đệm 15 phút của auditorium này");

        // 2) Một phim chỉ được chiếu tại 1 rạp trong cùng branch ở cùng khung giờ
        Integer movieId  = period.getMovie().getMovieID();
        Integer branchId = auditorium.getBranch().getId();
        long movieClash = showtimeRepo.countMovieOverlapInBranch(
                movieId, branchId, req.startTime(), req.endTime());
        if (movieClash > 0)
            throw new IllegalStateException("Phim này đã có suất chiếu khác trong cùng branch ở khung giờ trùng.");

        // Lưu
        var s = new Showtime();
        s.setPeriod(period);
        s.setAuditorium(auditorium);
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        s.setLanguage(req.language());
        s.setPrice(req.price());
        showtimeRepo.saveAndFlush(s);
        return mapper.toResponse(s);
    }

    @Transactional
    public ShowtimeResponse update(Integer id, ShowtimeUpdateRequest req) {
        var s = showtimeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found"));

        var period = periodRepo.findById(req.periodId())
                .orElseThrow(() -> new EntityNotFoundException("ScreeningPeriod not found"));
        var auditorium = auditoriumRepo.findById(req.auditoriumId())
                .orElseThrow(() -> new EntityNotFoundException("Auditorium not found"));

        if (!Objects.equals(period.getBranch().getId(), auditorium.getBranch().getId()))
            throw new IllegalArgumentException("Auditorium không thuộc cùng chi nhánh với ScreeningPeriod");

        if (!req.endTime().isAfter(req.startTime()))
            throw new IllegalArgumentException("EndTime phải lớn hơn StartTime");

        if (req.startTime().toLocalDate().isBefore(period.getStartDate())
                || req.endTime().toLocalDate().isAfter(period.getEndDate()))
            throw new IllegalArgumentException("Suất chiếu phải nằm trong khoảng ScreeningPeriod");

        var startWithBuf = req.startTime().minusMinutes(CLEANUP_MINUTES);
        var endWithBuf   = req.endTime().plusMinutes(CLEANUP_MINUTES);
        long roomClash = showtimeRepo.countOverlapsWithBufferExcluding(
                req.auditoriumId(), startWithBuf, endWithBuf, id);
        if (roomClash > 0)
            throw new IllegalStateException("Khung giờ vi phạm khoảng đệm 15 phút của auditorium này");

        Integer movieId  = period.getMovie().getMovieID();
        Integer branchId = auditorium.getBranch().getId();
        long movieClash = showtimeRepo.countMovieOverlapInBranchExcluding(
                movieId, branchId, req.startTime(), req.endTime(), id);
        if (movieClash > 0)
            throw new IllegalStateException("Phim này đã có suất chiếu khác trong cùng branch ở khung giờ trùng.");

        s.setPeriod(period);
        s.setAuditorium(auditorium);
        s.setStartTime(req.startTime());
        s.setEndTime(req.endTime());
        s.setLanguage(req.language());
        s.setPrice(req.price());
        showtimeRepo.save(s);
        return mapper.toResponse(s);
    }

    public void delete(Integer id) {
        if (!showtimeRepo.existsById(id)) return;
        showtimeRepo.deleteById(id);
    }

    // ========= LỊCH TUẦN =========

    public List<DayScheduleResponse> getNextWeekSchedule(Integer branchId) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonday = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
        return buildWeek(nextMonday, branchId);
    }

    public List<DayScheduleResponse> getWeekSchedule(LocalDate anchor, Integer branchId) {
        LocalDate base = (anchor != null) ? anchor : LocalDate.now();
        // chuẩn hoá về Monday của tuần chứa anchor
        LocalDate monday = base.minusDays((base.getDayOfWeek().getValue() + 6) % 7);
        return buildWeek(monday, branchId);
    }

    // ========= Helper =========
    private List<DayScheduleResponse> buildWeek(LocalDate monday, Integer branchId) {
        LocalDate sunday = monday.plusDays(7);
        LocalDateTime from = monday.atStartOfDay();
        LocalDateTime to   = sunday.atStartOfDay();

        List<Showtime> list = (branchId == null)
                ? showtimeRepo.findAllByStartTimeGreaterThanEqualAndStartTimeLessThan(from, to)
                : showtimeRepo.findAllByStartTimeGreaterThanEqualAndStartTimeLessThanAndAuditorium_Branch_Id(from, to, branchId);

        // group by date -> movie -> slots
        Map<LocalDate, Map<Integer, List<Showtime>>> grouped =
                list.stream().collect(Collectors.groupingBy(
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
