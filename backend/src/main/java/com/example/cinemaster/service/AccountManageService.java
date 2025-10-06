package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.AccountRequest;
import com.example.cinemaster.dto.response.AccountResponse;
import com.example.cinemaster.dto.response.PagedResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.entity.Role;
import com.example.cinemaster.mapper.AccountManageMapper;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.BranchRepository;
import com.example.cinemaster.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountManageService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final AccountManageMapper mapper;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    // CREATE
    public AccountResponse create(AccountRequest request, MultipartFile avatarFile) {
        Account account = mapper.toEntity(request);

        // Gán role
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found"));
        account.setRole(role);

        // Gán branch
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
        account.setBranch(branch);

        // Mã hóa password trước khi lưu
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            account.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        account.setIsActive(true);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = fileStorageService.saveFile(avatarFile);
            account.setAvatarUrl(avatarUrl);
        }

        accountRepository.save(account);
        return mapper.toResponse(account);
    }

    // UPDATE
    public AccountResponse update(Integer id, AccountRequest request, MultipartFile avatarFile) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        account.setFullName(request.getFullName());
        account.setPhoneNumber(request.getPhoneNumber());
        account.setAddress(request.getAddress());

        // Nếu có role mới
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new EntityNotFoundException("Role not found"));
            account.setRole(role);
        }

        // Nếu có branch mới
        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
            account.setBranch(branch);
        }

        // Nếu có password mới → encode lại
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            account.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String avatarUrl = fileStorageService.saveFile(avatarFile);
            account.setAvatarUrl(avatarUrl);
        }

        accountRepository.save(account);
        return mapper.toResponse(account);
    }

    // READ ALL
    public List<AccountResponse> getAll() {
        return accountRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    // READ BY ID
    public AccountResponse getById(Integer id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        return mapper.toResponse(account);
    }

    // DELETE (soft delete)
    public void softDelete(Integer id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        account.setIsActive(false);
        accountRepository.save(account);
    }

    // RESTORE (active lại)
    public void restore(Integer id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        account.setIsActive(true);
        accountRepository.save(account);
    }

    public PagedResponse<AccountResponse> getAllPaged(int page, int size,
                                                      Integer roleId,
                                                      Integer branchId,
                                                      String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "accountID"));

        Page<Account> accountPage = accountRepository.searchAccounts(
                keyword, roleId, branchId, pageable
        );

        List<AccountResponse> content = accountPage.getContent()
                .stream()
                .map(mapper::toResponse)
                .toList();

        return new PagedResponse<>(
                content,
                accountPage.getNumber(),
                accountPage.getSize(),
                accountPage.getTotalElements(),
                accountPage.getTotalPages()
        );
    }






}
