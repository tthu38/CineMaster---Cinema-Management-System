package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.ContactRequestRequest;
import com.example.cinemaster.dto.request.ContactUpdateRequest;
import com.example.cinemaster.dto.response.ContactRequestResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.ContactRequest;
import com.example.cinemaster.mapper.ContactRequestMapper;
import com.example.cinemaster.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactRequestService {

    private final ContactRequestRepository contactRepo;
    private final ContactRequestMapper mapper;
    private final BranchRepository branchRepo;
    private final AccountRepository accountRepo;

    public ContactRequestResponse create(ContactRequestRequest dto) {
        log.info(" Creating contact: {}", dto);

        ContactRequest entity = mapper.toEntity(dto);
        entity.setStatus("Pending");

        if (dto.getBranchId() != null) {
            var branch = branchRepo.findByIdAndIsActiveTrue(dto.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh hoạt động!"));
            entity.setBranch(branch);
        }

        contactRepo.save(entity);
        return mapper.toResponse(entity);
    }

    // Lấy danh sách theo chi nhánh (cho Staff)
    public List<ContactRequestResponse> getByBranch(Integer branchId) {
        return contactRepo.findByBranch_Id(branchId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    //  Nhân viên xử lý liên hệ
    public ContactRequestResponse updateStatus(Integer contactId, ContactUpdateRequest dto, Integer staffId) {
        ContactRequest contact = contactRepo.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ #" + contactId));

        Account staff = accountRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên #" + staffId));

        //  Chỉ xử lý liên hệ cùng chi nhánh
        if (contact.getBranch() == null ||
                !staff.getBranch().getId().equals(contact.getBranch().getId())) {
            throw new RuntimeException("Bạn không thể xử lý liên hệ của chi nhánh khác!");
        }

        contact.setStatus(dto.getStatus());
        contact.setHandleNote(dto.getHandleNote());
        contact.setHandledBy(staff);
        contact.setHandledAt(LocalDateTime.now());

        contactRepo.save(contact);
        return mapper.toResponse(contact);
    }

    public ContactRequestResponse getById(Integer id) {
        ContactRequest contact = contactRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy liên hệ #" + id));
        return mapper.toResponse(contact);
    }

    public List<ContactRequestResponse> getAll() {
        return contactRepo.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }



}
