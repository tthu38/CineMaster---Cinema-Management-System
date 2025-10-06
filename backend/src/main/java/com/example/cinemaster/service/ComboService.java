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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComboService {

    private final ComboRepository comboRepository;
    private final BranchRepository branchRepository;
    private final ComboMapper comboMapper;
    private final FileStorageService fileStorageService;

    // CREATE
    public ComboResponse create(ComboRequest request, MultipartFile imageFile) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));

        Combo combo = comboMapper.toEntity(request);
        combo.setBranchID(branch);
        combo.setAvailable(true);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.saveFile(imageFile);
            combo.setImageURL(imageUrl);
        }

        comboRepository.save(combo);
        return comboMapper.toResponse(combo);
    }

    // READ ALL
    public List<ComboResponse> getAll() {
        List<Combo> combos = comboRepository.findAllOrderByAvailable();
        return comboMapper.toResponseList(combos);
    }

    // READ BY ID
    public ComboResponse getById(Integer id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Combo not found"));
        return comboMapper.toResponse(combo);
    }

    // UPDATE
    public ComboResponse update(Integer id, ComboRequest request, MultipartFile imageFile) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Combo not found"));

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));

        // ✅ Giữ ảnh cũ trước khi mapper ghi đè
        String oldImageUrl = combo.getImageURL();

        comboMapper.updateComboFromRequest(request, combo);
        combo.setBranchID(branch);

        // ✅ Nếu không upload ảnh mới → giữ lại ảnh cũ
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.saveFile(imageFile);
            combo.setImageURL(imageUrl);
        } else {
            combo.setImageURL(oldImageUrl);
        }

        comboRepository.save(combo);
        return comboMapper.toResponse(combo);
    }

    // SOFT DELETE
    public void delete(Integer id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Combo not found"));

        if (Boolean.FALSE.equals(combo.getAvailable())) {
            throw new IllegalStateException("Combo already inactive");
        }

        combo.setAvailable(false);
        comboRepository.save(combo);
    }

    // RESTORE
    public ComboResponse restore(Integer id) {
        Combo combo = comboRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Combo not found"));

        if (Boolean.TRUE.equals(combo.getAvailable())) {
            throw new IllegalStateException("Combo already active");
        }

        combo.setAvailable(true);
        comboRepository.save(combo);
        return comboMapper.toResponse(combo);
    }
}
