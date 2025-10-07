package com.example.cinemaster.service;


import com.example.cinemaster.dto.request.AuditoriumRequest;
import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.repository.AuditoriumRepository;
import com.example.cinemaster.repository.BranchRepository; // Giả định Repository này tồn tại
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuditoriumService {

    private final AuditoriumRepository auditoriumRepository;
    private final BranchRepository branchRepository;

    private static final String NOT_FOUND = "Phòng chiếu không tìm thấy với ID: ";
    private static final String INACTIVE_STATUS = "Phòng chiếu không hoạt động hoặc không tồn tại với ID: ";

    public AuditoriumService(AuditoriumRepository auditoriumRepository, BranchRepository branchRepository) {
        this.auditoriumRepository = auditoriumRepository;
        this.branchRepository = branchRepository;
    }

    // --- HÀM MAPPER (ĐÃ THÊM ISACTIVE) ---
    private AuditoriumResponse mapToResponse(Auditorium auditorium) {
        Integer branchID = auditorium.getBranch() != null ? auditorium.getBranch().getId() : null;
        String branchName = auditorium.getBranch() != null ? auditorium.getBranch().getBranchName() : "Không tìm thấy";

        return AuditoriumResponse.builder()
                .auditoriumID(auditorium.getAuditoriumID())
                .name(auditorium.getName())
                .capacity(auditorium.getCapacity())
                .type(auditorium.getType())
                .branchId(branchID)
                .branchName(branchName)
                // ✨ THÊM ISACTIVE
                .isActive(auditorium.getIsActive())
                .build();
    }

    // --- READ ALL (CHO ADMIN/MANAGER) ---
    public List<AuditoriumResponse> getAllAuditoriums() {
        // Lấy tất cả, bao gồm cả đã đóng
        return auditoriumRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- READ ALL ACTIVE (CHO CLIENT/STAFF) ---
    public List<AuditoriumResponse> getAllActiveAuditoriums() {
        // Chỉ lấy các phòng chiếu đang hoạt động
        return auditoriumRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- READ ONE (CHO ADMIN - xem được cả đã đóng) ---
    public AuditoriumResponse getAuditoriumByIdForAdmin(Integer id) {
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND + id));
        return mapToResponse(auditorium);
    }

    // --- READ ONE (CHO CLIENT/STAFF - chỉ xem cái đang hoạt động) ---
    public AuditoriumResponse getAuditoriumByIdForClient(Integer id) {
        // Sử dụng phương thức lọc mới
        Auditorium auditorium = auditoriumRepository.findByAuditoriumIDAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException(INACTIVE_STATUS + id));
        return mapToResponse(auditorium);
    }

    // --- READ BY BRANCH ID (CHO ADMIN - xem cả đã đóng theo Branch) ---
    @Transactional(readOnly = true)
    public List<AuditoriumResponse> getAuditoriumsByBranchId(Integer branchId) {
        return auditoriumRepository.findByBranch_Id(branchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // --- READ BY BRANCH ID (CHO CLIENT/STAFF - chỉ xem đang hoạt động) ---
    @Transactional(readOnly = true)
    public List<AuditoriumResponse> getActiveAuditoriumsByBranchId(Integer branchId) {
        // Sử dụng phương thức lọc mới
        return auditoriumRepository.findByBranch_IdAndIsActiveTrue(branchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // 3. CREATE (Giữ nguyên)
    public AuditoriumResponse createAuditorium(AuditoriumRequest request) {
        // ... (Logic tạo giữ nguyên) ...
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Chi nhánh không tìm thấy với ID: " + request.getBranchId()));

        Auditorium auditorium = Auditorium.builder()
                .name(request.getName())
                .capacity(request.getCapacity())
                .type(request.getType())
                .branch(branch)
                .build();

        Auditorium created = auditoriumRepository.save(auditorium);
        return mapToResponse(created);
    }

    // 4. UPDATE
    public AuditoriumResponse updateAuditorium(Integer id, AuditoriumRequest request) {
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND + id));

        // LƯU Ý: Không cho phép cập nhật nếu phòng chiếu đã bị vô hiệu hóa
        if (!auditorium.getIsActive()) {
            throw new RuntimeException("Cannot update inactive Auditorium with ID: " + id);
        }

        // Kiểm tra và cập nhật Branch nếu ID thay đổi
        if (!auditorium.getBranch().getId().equals(request.getBranchId())) {
            Branch newBranch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Chi nhánh mới không tìm thấy với ID: " + request.getBranchId()));
            auditorium.setBranch(newBranch);
        }

        // Cập nhật các trường khác
        auditorium.setName(request.getName());
        auditorium.setCapacity(request.getCapacity());
        auditorium.setType(request.getType());

        Auditorium updated = auditoriumRepository.save(auditorium);
        return mapToResponse(updated);
    }

    // 5. DEACTIVATE (Thay thế DELETE)
    public void deactivateAuditorium(Integer id) {
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND + id));

        if (!auditorium.getIsActive()) {
            throw new RuntimeException("Phòng chiếu ID " + id + " đã đóng rồi.");
        }

        auditorium.setIsActive(false);
        // LƯU Ý NGHIỆP VỤ: Cần thêm logic vô hiệu hóa các Showtimes (Lịch Chiếu) liên quan tại đây

        auditoriumRepository.save(auditorium);
    }

    // 6. ACTIVATE (Kích hoạt lại)
    public void activateAuditorium(Integer id) {
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND + id));

        if (auditorium.getIsActive()) {
            throw new RuntimeException("Phòng chiếu ID " + id + " đang hoạt động rồi.");
        }

        auditorium.setIsActive(true);
        // LƯU Ý NGHIỆP VỤ: Có thể cần logic để kiểm tra lại Branch (Chi nhánh) có đang hoạt động hay không

        auditoriumRepository.save(auditorium);
    }
}