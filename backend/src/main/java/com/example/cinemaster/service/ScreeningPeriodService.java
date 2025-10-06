package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.ScreeningPeriodRequest;
import com.example.cinemaster.dto.response.ScreeningPeriodResponse;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.entity.ScreeningPeriod;
import com.example.cinemaster.exception.ResourceNotFoundException;
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

    // --- HÀM MAPPING Entity sang Response (Giữ nguyên) ---
    private ScreeningPeriodResponse toResponse(ScreeningPeriod entity) {
        return ScreeningPeriodResponse.builder()
                .id(entity.getId())
                .movieId(entity.getMovie().getMovieID())
                .movieTitle(entity.getMovie().getTitle())
                .branchId(entity.getBranch().getId())
                .branchName(entity.getBranch().getBranchName())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                // 🔥 KHẮC PHỤC: THÊM ISACTIVE VÀO RESPONSE
                .isActive(entity.getIsActive())
                .build();
    }

    // --- 1. CREATE (Giữ nguyên) ---
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
                .build();

        ScreeningPeriod savedPeriod = screeningPeriodRepository.save(newPeriod);
        return toResponse(savedPeriod);
    }

    // --- 2. READ ALL (Giữ nguyên) ---
    @Transactional(readOnly = true)
    public List<ScreeningPeriodResponse> getAll() {
        return screeningPeriodRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // --- 3. READ BY ID (Giữ nguyên) ---
    @Transactional(readOnly = true)
    public ScreeningPeriodResponse getById(Integer id) {
        ScreeningPeriod period = screeningPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening Period not found with ID: " + id));
        return toResponse(period);
    }

    // --- 4. UPDATE (Giữ nguyên) ---
    @Transactional
    public ScreeningPeriodResponse update(Integer id, ScreeningPeriodRequest request) {
        ScreeningPeriod existingPeriod = screeningPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening Period not found with ID: " + id));

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with ID: " + request.getMovieId()));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + request.getBranchId()));

        existingPeriod.setMovie(movie);
        existingPeriod.setBranch(branch);
        existingPeriod.setStartDate(request.getStartDate());
        existingPeriod.setEndDate(request.getEndDate());

        // 🔥 KHẮC PHỤC QUAN TRỌNG: Thêm dòng cập nhật trạng thái
        // Đảm bảo ScreeningPeriodRequest có getter cho isActive
        if (request.getIsActive() != null) {
            existingPeriod.setIsActive(request.getIsActive());
        }

        ScreeningPeriod updatedPeriod = screeningPeriodRepository.save(existingPeriod);
        return toResponse(updatedPeriod);
    }

    // ----------------------------------------------------------------------
    // --- 5. DELETE (CẬP NHẬT: Xóa Cứng / Xóa Mềm theo ngày) ---
    // ----------------------------------------------------------------------
    @Transactional
    public void delete(Integer id) {
        ScreeningPeriod period = screeningPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening Period not found with ID: " + id));

        LocalDate today = LocalDate.now();

        // 1. Logic Xóa Cứng: Nếu ngày bắt đầu chiếu ở TƯƠNG LAI (chưa bắt đầu)
        if (period.getStartDate().isAfter(today)) {
            screeningPeriodRepository.delete(period);
            return;
        }

        // 2. Logic Xóa Mềm: Nếu đang chiếu hoặc đã kết thúc (KHÔNG phải tương lai)
        if (Boolean.TRUE.equals(period.getIsActive())) {
            period.setIsActive(false);
            screeningPeriodRepository.save(period);
        }
    }

    // --- 6. READ BY BRANCH ID (Giữ nguyên) ---
    @Transactional(readOnly = true)
    public List<ScreeningPeriodResponse> getByBranchId(Integer branchId) {
        return screeningPeriodRepository.findByBranch_Id(branchId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------------
    // --- 7. LOGIC THÁC ĐỔ: VÔ HIỆU HÓA HÀNG LOẠT THEO BRANCH (MỚI) ---
    // ----------------------------------------------------------------------
    /**
     * Vô hiệu hóa tất cả ScreeningPeriods của một Branch khi Branch đó đóng.
     * @param branchId ID của Branch không hoạt động.
     */
    @Transactional
    public void deactivatePeriodsByBranch(Integer branchId) {
        List<ScreeningPeriod> periodsToDeactivate = screeningPeriodRepository.findByBranch_Id(branchId);

        periodsToDeactivate.forEach(period -> {
            if (Boolean.TRUE.equals(period.getIsActive())) {
                period.setIsActive(false);
                screeningPeriodRepository.save(period);
            }
        });
        System.out.println("LOG: Deactivated " + periodsToDeactivate.size() + " screening periods for Branch ID: " + branchId);
    }
}