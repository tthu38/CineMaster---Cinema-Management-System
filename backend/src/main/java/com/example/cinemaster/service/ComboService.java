package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.ComboRequest;
import com.example.cinemaster.dto.response.ComboResponse;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.entity.Combo;
import com.example.cinemaster.mapper.ComboMapper;
import com.example.cinemaster.repository.BranchRepository;
import com.example.cinemaster.repository.ComboRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComboService {

    private final ComboRepository comboRepository;
    private final BranchRepository branchRepository;
    private final ComboMapper comboMapper;
    private final FileStorageService fileStorageService;

    // ===== CREATE =====
    public ComboResponse create(ComboRequest request, MultipartFile imageFile) {
        if (request.getBranchId() == null) {
            throw new IllegalArgumentException("Chi nhánh không được để trống.");
        }

        log.info("🟢 Creating new combo for branch {}", request.getBranchId());
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Chi nhánh không tồn tại."));

        Combo combo = comboMapper.toEntity(request);
        combo.setBranchID(branch);
        combo.setAvailable(true);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.saveFile(imageFile);
            combo.setImageURL(imageUrl);
        }

        comboRepository.save(combo);
        log.info("✅ Created combo: {} (Branch: {})", combo.getNameCombo(), branch.getBranchName());
        return comboMapper.toResponse(combo);
    }

    // ===== READ ALL =====
    public List<ComboResponse> getAll() {
        log.info("🔍 Loading all combos (sorted by available)");
        List<Combo> combos = comboRepository.findAllOrderByAvailable();
        return comboMapper.toResponseList(combos);
    }

    // ===== READ BY ID =====
    public ComboResponse getById(Integer id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy combo ID: " + id));
        return comboMapper.toResponse(combo);
    }

    // ===== READ BY BRANCH =====
    public List<ComboResponse> getByBranch(Integer branchId) {
        log.info("🔍 Loading combos for branch ID: {}", branchId);
        List<Combo> combos = comboRepository.findByBranchId(branchId);
        return comboMapper.toResponseList(combos);
    }

    // ===== READ AVAILABLE ONLY =====
    public List<ComboResponse> getAvailable() {
        log.info("🔍 Loading available combos for frontend display");
        List<Combo> combos = comboRepository.findAvailableCombos();
        return comboMapper.toResponseList(combos);
    }

    // ===== UPDATE =====
    public ComboResponse update(Integer id, ComboRequest request, MultipartFile imageFile) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy combo ID: " + id));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Chi nhánh không tồn tại."));

        String oldImageUrl = combo.getImageURL();

        comboMapper.updateComboFromRequest(request, combo);
        combo.setBranchID(branch);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.saveFile(imageFile);
            combo.setImageURL(imageUrl);
        } else {
            combo.setImageURL(oldImageUrl);
        }

        comboRepository.save(combo);
        log.info("🟢 Updated combo ID {} (Branch: {})", id, branch.getBranchName());
        return comboMapper.toResponse(combo);
    }

    // ===== SOFT DELETE =====
    public void delete(Integer id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy combo ID: " + id));

        if (Boolean.FALSE.equals(combo.getAvailable())) {
            throw new IllegalStateException("Combo này đã bị vô hiệu hóa trước đó.");
        }

        combo.setAvailable(false);
        comboRepository.save(combo);
        log.warn("🟠 Combo ID {} has been deactivated.", id);
    }

    // ===== RESTORE =====
    public ComboResponse restore(Integer id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy combo ID: " + id));

        if (Boolean.TRUE.equals(combo.getAvailable())) {
            throw new IllegalStateException("Combo này đã được kích hoạt.");
        }

        combo.setAvailable(true);
        comboRepository.save(combo);
        log.info("🟢 Restored combo ID {}", id);
        return comboMapper.toResponse(combo);
    }
}
