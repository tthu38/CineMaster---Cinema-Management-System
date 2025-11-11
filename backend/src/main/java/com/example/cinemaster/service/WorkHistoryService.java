// src/main/java/com/example/cinemaster/service/WorkHistoryService.java
package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.WorkHistoryCreateRequest;
import com.example.cinemaster.dto.request.WorkHistoryUpdateRequest;
import com.example.cinemaster.dto.response.PageResponse;
import com.example.cinemaster.dto.response.WorkHistoryResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.WorkHistory;
import com.example.cinemaster.mapper.WorkHistoryMapper;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.WorkHistoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WorkHistoryService {

    private final WorkHistoryRepository workHistoryRepo;
    private final AccountRepository accountRepo;

    @Transactional
    public WorkHistoryResponse create(WorkHistoryCreateRequest req) {
        Account actor = accountRepo.findById(req.accountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + req.accountId()));

        Account approver = null;
        if (req.affectedAccountId() != null) {
            approver = accountRepo.findById(req.affectedAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Affected account not found: " + req.affectedAccountId()));
        }

        WorkHistory entity = WorkHistory.builder()
                .accountID(actor)
                .affectedAccountID(approver)
                .action(req.action())
                .actionTime(req.actionTime() != null ? req.actionTime() : Instant.now())
                .description(req.description())
                .build();

        return WorkHistoryMapper.toResponse(workHistoryRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public WorkHistoryResponse getById(Integer id) {
        WorkHistory e = workHistoryRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WorkHistory not found: " + id));
        return WorkHistoryMapper.toResponse(e);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkHistoryResponse> search(Integer accountId, Integer affectedAccountId,
                                                    Instant from, Instant to, Pageable pageable) {

        Specification<WorkHistory> spec = Specification.allOf(); // Spring Data JPA 3.2+

        if (accountId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("accountID").get("id"), accountId));
        }
        if (affectedAccountId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("affectedAccountID").get("id"), affectedAccountId));
        }
        if (from != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("actionTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("actionTime"), to));
        }

        Page<WorkHistory> page = workHistoryRepo.findAll(spec, pageable);
        var items = page.map(WorkHistoryMapper::toResponse).toList();

        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional
    public WorkHistoryResponse update(Integer id, WorkHistoryUpdateRequest req) {
        WorkHistory e = workHistoryRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WorkHistory not found: " + id));

        if (req.affectedAccountId() != null) {
            Account approver = accountRepo.findById(req.affectedAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Affected account not found: " + req.affectedAccountId()));
            e.setAffectedAccountID(approver);
        }
        if (req.action() != null) e.setAction(req.action());
        if (req.description() != null) e.setDescription(req.description());
        if (req.actionTime() != null) e.setActionTime(req.actionTime());

        return WorkHistoryMapper.toResponse(workHistoryRepo.save(e));
    }

    @Transactional
    public void delete(Integer id) {
        if (!workHistoryRepo.existsById(id)) {
            throw new EntityNotFoundException("WorkHistory not found: " + id);
        }
        workHistoryRepo.deleteById(id);
    }
}
