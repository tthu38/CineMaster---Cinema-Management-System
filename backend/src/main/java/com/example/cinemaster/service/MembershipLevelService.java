package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.MembershipLevelRequest;
import com.example.cinemaster.dto.response.MembershipLevelResponse;
import com.example.cinemaster.dto.response.PageResponse;
import com.example.cinemaster.entity.MembershipLevel;
import com.example.cinemaster.exception.NotFoundException;
import com.example.cinemaster.mapper.MembershipLevelMapper;
import com.example.cinemaster.repository.MembershipLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipLevelService {

    private final MembershipLevelRepository repository;
    private final MembershipLevelMapper mapper;

    private void validatePointsRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("MinPoints phải <= MaxPoints.");
        }
    }

    private MembershipLevel getOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("MembershipLevel không tồn tại: " + id));
    }

    @Transactional
    public MembershipLevelResponse create(MembershipLevelRequest request) {
        validatePointsRange(request.getMinPoints(), request.getMaxPoints());
        if (repository.existsByLevelNameIgnoreCase(request.getLevelName())) {
            throw new IllegalArgumentException("LevelName đã tồn tại.");
        }
        MembershipLevel entity = mapper.toEntity(request);
        entity = repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public MembershipLevelResponse get(Integer id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MembershipLevelResponse> list(Pageable pageable) {
        Page<MembershipLevel> page = repository.findAll(pageable);
        List<MembershipLevelResponse> content = page.getContent()
                .stream()
                .map(mapper::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional
    public MembershipLevelResponse update(Integer id, MembershipLevelRequest request) {
        validatePointsRange(request.getMinPoints(), request.getMaxPoints());
        if (request.getLevelName() != null &&
                repository.existsByLevelNameIgnoreCaseAndIdNot(request.getLevelName(), id)) {
            throw new IllegalArgumentException("LevelName đã tồn tại.");
        }
        MembershipLevel entity = getOrThrow(id);
        mapper.updateEntity(entity, request);
        entity = repository.save(entity);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("MembershipLevel không tồn tại: " + id);
        }
        repository.deleteById(id);
    }
}
