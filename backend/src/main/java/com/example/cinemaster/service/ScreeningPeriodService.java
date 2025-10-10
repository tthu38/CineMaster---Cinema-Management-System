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

@Service
@RequiredArgsConstructor
public class ScreeningPeriodService {

    private final ScreeningPeriodRepository screeningPeriodRepository;
    private final MovieRepository movieRepository;
    private final BranchRepository branchRepository;
    private final ScreeningPeriodMapper mapper;

    // ----------------------------------------------------------------------
    // --- 1. FIND ACTIVE (gộp từ file đầu tiên)
    // ----------------------------------------------------------------------
    public List<ScreeningPeriod> findActive(Integer branchId, LocalDate onDate) {
        // Trả về tất cả period đang bao phủ ngày onDate
        return screeningPeriodRepository.findActive(branchId, onDate, onDate);
    }

    // ----------------------------------------------------------------------
    // --- 2. CREATE
    // ----------------------------------------------------------------------
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

    // ----------------------------------------------------------------------
    // --- 3. READ ALL
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ScreeningPeriodResponse> getAll() {
        return mapper.toLiteList(screeningPeriodRepository.findAll());
    }

    // ----------------------------------------------------------------------
    // --- 4. READ BY ID
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ScreeningPeriodResponse getById(Integer id) {
        ScreeningPeriod period = screeningPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening Period not found with ID: " + id));
        return mapper.toLite(period);
    }

    // ----------------------------------------------------------------------
    // --- 5. UPDATE
    // ----------------------------------------------------------------------
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


    // ----------------------------------------------------------------------
    // --- 6. DELETE (Cứng / Mềm)
    // ----------------------------------------------------------------------
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

    // ----------------------------------------------------------------------
    // --- 7. READ BY BRANCH ID
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<ScreeningPeriodResponse> getByBranchId(Integer branchId) {
        return mapper.toLiteList(screeningPeriodRepository.findByBranch_Id(branchId));
    }

    // ----------------------------------------------------------------------
    // --- 8. DEACTIVATE ALL BY BRANCH (THÁC ĐỔ)
    // ----------------------------------------------------------------------
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
}
