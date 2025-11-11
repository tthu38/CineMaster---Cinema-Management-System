package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.AuditoriumRequest;
import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.mapper.AuditoriumMapper;
import com.example.cinemaster.repository.AuditoriumRepository;
import com.example.cinemaster.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AuditoriumService {

    private final AuditoriumRepository auditoriumRepository;
    private final BranchRepository branchRepository;
    private final AuditoriumMapper mapper;

    private static final String NOT_FOUND = "Phòng chiếu không tìm thấy với ID: ";
    private static final String INACTIVE_STATUS = "Phòng chiếu không hoạt động hoặc không tồn tại với ID: ";

    public List<AuditoriumResponse> getAllAuditoriums() {
        return mapper.toResponseList(auditoriumRepository.findAll());
    }

    public List<AuditoriumResponse> getAllActiveAuditoriums() {
        return mapper.toResponseList(auditoriumRepository.findByIsActiveTrue());
    }

    public AuditoriumResponse getAuditoriumByIdForAdmin(Integer id) {
        return mapper.toResponse(auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND + id)));
    }

    public AuditoriumResponse getAuditoriumByIdForClient(Integer id) {
        return mapper.toResponse(auditoriumRepository.findByAuditoriumIDAndIsActiveTrue(id)
                .orElseThrow(() -> new EntityNotFoundException(INACTIVE_STATUS + id)));
    }

    public List<AuditoriumResponse> getAuditoriumsByBranchId(Integer branchId) {
        return mapper.toResponseList(auditoriumRepository.findByBranch_Id(branchId));
    }

    public List<AuditoriumResponse> getActiveAuditoriumsByBranchId(Integer branchId) {
        return mapper.toResponseList(auditoriumRepository.findByBranch_IdAndIsActiveTrue(branchId));
    }

    public AuditoriumResponse createAuditorium(AuditoriumRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Chi nhánh không tìm thấy với ID: " + request.getBranchId()));

        Auditorium entity = Auditorium.builder()
                .name(request.getName())
                .capacity(request.getCapacity())
                .type(request.getType())
                .branch(branch)
                .isActive(true)
                .build();

        return mapper.toResponse(auditoriumRepository.save(entity));
    }

    public AuditoriumResponse updateAuditorium(Integer id, AuditoriumRequest request) {
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND + id));

        if (!auditorium.getIsActive()) {
            throw new RuntimeException("Không thể cập nhật phòng chiếu đã bị vô hiệu hóa (ID: " + id + ")");
        }

        if (!auditorium.getBranch().getId().equals(request.getBranchId())) {
            Branch newBranch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Chi nhánh mới không tìm thấy với ID: " + request.getBranchId()));
            auditorium.setBranch(newBranch);
        }

        auditorium.setName(request.getName());
        auditorium.setCapacity(request.getCapacity());
        auditorium.setType(request.getType());

        return mapper.toResponse(auditoriumRepository.save(auditorium));
    }

    public void deactivateAuditorium(Integer id) {
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND + id));

        if (!auditorium.getIsActive()) {
            throw new RuntimeException("Phòng chiếu ID " + id + " đã bị đóng rồi.");
        }

        auditorium.setIsActive(false);
        auditoriumRepository.save(auditorium);
    }

    public void activateAuditorium(Integer id) {
        Auditorium auditorium = auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND + id));

        if (auditorium.getIsActive()) {
            throw new RuntimeException("Phòng chiếu ID " + id + " đang hoạt động rồi.");
        }

        auditorium.setIsActive(true);
        auditoriumRepository.save(auditorium);
    }

    public List<AuditoriumResponse> listByBranch(Integer branchId) {
        List<Auditorium> list = (branchId == null)
                ? auditoriumRepository.findAll()
                : auditoriumRepository.findByBranch_IdAndIsActiveTrue(branchId);
        return mapper.toResponseList(list);
    }
}
