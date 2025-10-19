package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.ScreeningPeriodRequest;
import com.example.cinemaster.dto.response.ScreeningPeriodResponse;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.entity.ScreeningPeriod;
import com.example.cinemaster.exception.ResourceNotFoundException;
import com.example.cinemaster.mapper.ScreeningPeriodMapper;
import com.example.cinemaster.repository.BranchRepository;
import com.example.cinemaster.repository.MovieRepository;
import com.example.cinemaster.repository.ScreeningPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScreeningPeriodService {

    private final ScreeningPeriodRepository screeningPeriodRepository;
    private final MovieRepository movieRepository;
    private final BranchRepository branchRepository;
    private final ScreeningPeriodMapper mapper;

    public List<ScreeningPeriod> findActive(Integer branchId, LocalDate onDate) {
        return screeningPeriodRepository.findActive(branchId, onDate);
    }


    @Transactional
    public ScreeningPeriodResponse create(ScreeningPeriodRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with ID: " + request.getMovieId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + request.getBranchId()));

        ScreeningPeriod newPeriod = ScreeningPeriod.builder()
                .movie(movie)
                .branch(branch)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();

        return mapper.toLite(screeningPeriodRepository.save(newPeriod));
    }

    @Transactional(readOnly = true)
    public List<ScreeningPeriodResponse> getAll() {
        return mapper.toLiteList(screeningPeriodRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ScreeningPeriodResponse getById(Integer id) {
        ScreeningPeriod period = screeningPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening Period not found with ID: " + id));
        return mapper.toLite(period);
    }

    @Transactional
    public ScreeningPeriodResponse update(Integer id, ScreeningPeriodRequest request) {
        ScreeningPeriod existingPeriod = screeningPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening Period not found with ID: " + id));

        // Kiểm tra FK
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with ID: " + request.getMovieId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + request.getBranchId()));

        // ⚡ MapStruct tự cập nhật các field không null
        mapper.updateEntityFromDto(request, existingPeriod);

        // Gán lại quan hệ phức tạp (nếu request chỉ gửi ID)
        existingPeriod.setMovie(movie);
        existingPeriod.setBranch(branch);

        ScreeningPeriod updated = screeningPeriodRepository.save(existingPeriod);
        return mapper.toLite(updated);
    }

    @Transactional
    public void delete(Integer id) {
        ScreeningPeriod period = screeningPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening Period not found with ID: " + id));

        LocalDate today = LocalDate.now();

        if (period.getStartDate().isAfter(today)) {
            screeningPeriodRepository.delete(period); // Xóa cứng
        } else if (Boolean.TRUE.equals(period.getIsActive())) {
            period.setIsActive(false); // Xóa mềm
            screeningPeriodRepository.save(period);
        }
    }

    @Transactional(readOnly = true)
    public List<ScreeningPeriodResponse> getByBranchId(Integer branchId) {
        return mapper.toLiteList(screeningPeriodRepository.findByBranch_Id(branchId));
    }

    @Transactional
    public void deactivatePeriodsByBranch(Integer branchId) {
        List<ScreeningPeriod> periodsToDeactivate = screeningPeriodRepository.findByBranch_Id(branchId);

        periodsToDeactivate.forEach(period -> {
            if (Boolean.TRUE.equals(period.getIsActive())) {
                period.setIsActive(false);
                screeningPeriodRepository.save(period);
            }
        });

        System.out.println("LOG: Deactivated " + periodsToDeactivate.size()
                + " screening periods for Branch ID: " + branchId);
    }
    @Transactional(readOnly = true)
    public List<Movie> getMoviesNowShowingByBranchId(Integer branchId) {
        // 1️⃣ Gọi trực tiếp repository
        List<Movie> movies = screeningPeriodRepository.findNowShowingMoviesByBranchId(branchId);

        // 2️⃣ Ghi log để debug
        System.out.println("🎬 [DEBUG] Số lượng phim đang chiếu tìm thấy tại BranchID = "
                + branchId + " → " + movies.size());

        // 3️⃣ Trả về kết quả
        return movies;
    }

    public List<Movie> getComingSoonMovies() {
        return screeningPeriodRepository.findComingSoon()
                .stream()
                .map(ScreeningPeriod::getMovie)
                .distinct()
                .collect(Collectors.toList());
    }

    // 🔹 Lấy tất cả phim có kỳ chiếu
    public List<Movie> getAllMoviesWithPeriods() {
        return screeningPeriodRepository.findAll()
                .stream()
                .map(ScreeningPeriod::getMovie)
                .distinct()
                .collect(Collectors.toList());
    }

    // 🔹 Lấy kỳ chiếu hiện tại của một phim
    public ScreeningPeriod getCurrentPeriodByMovie(Integer movieId) {
        return screeningPeriodRepository.findByMovie_MovieID(movieId)
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .findFirst()
                .orElse(null);
    }

}
