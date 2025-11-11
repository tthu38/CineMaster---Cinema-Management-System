package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.BranchRequest;
import com.example.cinemaster.dto.response.BranchResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.AuditoriumRepository;
import com.example.cinemaster.repository.BranchRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BranchService {
    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private ScreeningPeriodService screeningPeriodService;

    private static final String BRANCH_NOT_FOUND = "Không tìm thấy nhánh có ID:";
    private static final String MANAGER_NOT_FOUND = "Không tìm thấy người quản lý có ID:";

    private Account getManagerAccount(Integer managerId) {
        if (managerId == null) return null;
        return accountRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException(MANAGER_NOT_FOUND + managerId));
    }

    private BranchResponse convertToDTO(Branch branch) {
        BranchResponse dto = new BranchResponse();
        dto.setBranchId(branch.getId());
        dto.setBranchName(branch.getBranchName());
        dto.setAddress(branch.getAddress());
        dto.setPhone(branch.getPhone());
        dto.setEmail(branch.getEmail());

        if (branch.getManager() != null) {
            dto.setManagerId(branch.getManager().getAccountID());
            dto.setManagerName(branch.getManager().getFullName());
        } else {
            dto.setManagerId(null);
            dto.setManagerName("N/A");
        }

        dto.setOpenTime(branch.getOpenTime());
        dto.setCloseTime(branch.getCloseTime());
        dto.setIsActive(branch.getIsActive());

        return dto;
    }

    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<BranchResponse> getAllActiveBranches() {
        return branchRepository.findByIsActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public BranchResponse getBranchByIdForAdmin(Integer id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(BRANCH_NOT_FOUND + id));
        return convertToDTO(branch);
    }

    public BranchResponse getBranchByIdForClient(Integer id) {
        Branch branch = branchRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException(BRANCH_NOT_FOUND + id + " or is inactive"));
        return convertToDTO(branch);
    }

    public BranchResponse createBranch(BranchRequest requestDTO) {
        Branch branch = new Branch();
        Account managerAccount = getManagerAccount(requestDTO.getManagerId());

        branch.setBranchName(requestDTO.getBranchName());
        branch.setAddress(requestDTO.getAddress());
        branch.setPhone(requestDTO.getPhone());
        branch.setEmail(requestDTO.getEmail());
        branch.setManager(managerAccount);
        branch.setOpenTime(requestDTO.getOpenTime());
        branch.setCloseTime(requestDTO.getCloseTime());

        Branch savedBranch = branchRepository.save(branch);
        return convertToDTO(savedBranch);
    }

    public BranchResponse updateBranch(Integer id, BranchRequest requestDTO) {
        Branch existingBranch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(BRANCH_NOT_FOUND + id));

        Account managerAccount = getManagerAccount(requestDTO.getManagerId());

        existingBranch.setBranchName(requestDTO.getBranchName());
        existingBranch.setAddress(requestDTO.getAddress());
        existingBranch.setPhone(requestDTO.getPhone());
        existingBranch.setEmail(requestDTO.getEmail());
        existingBranch.setManager(managerAccount);
        existingBranch.setOpenTime(requestDTO.getOpenTime());
        existingBranch.setCloseTime(requestDTO.getCloseTime());

        Branch updatedBranch = branchRepository.save(existingBranch);
        return convertToDTO(updatedBranch);
    }

    @Transactional
    public void deactivateBranch(Integer id) {
        Branch existingBranch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(BRANCH_NOT_FOUND + id));

        if (Boolean.FALSE.equals(existingBranch.getIsActive())) {
            return;
        }

        existingBranch.setIsActive(false);
        branchRepository.save(existingBranch);

        int closedAuditoriums = auditoriumRepository.updateIsActiveStatusByBranchId(id, false);
        System.out.println("LOG: Deactivated " + closedAuditoriums + " auditoriums for Branch ID: " + id);

        screeningPeriodService.deactivatePeriodsByBranch(id);
    }

    @Transactional
    public void activateBranch(Integer id) {
        Branch existingBranch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(BRANCH_NOT_FOUND + id));

        if (Boolean.TRUE.equals(existingBranch.getIsActive())) {
            return;
        }

        existingBranch.setIsActive(true);
        branchRepository.save(existingBranch);

        System.out.println("LOG: Branch ID " + id + " activated. Related entities remain inactive for manual setup.");
    }

    //giang
    public List<BranchResponse> getBranchesByMovie(Integer movieId) {
        List<Branch> branches = branchRepository.findBranchesByMovie(movieId);
        return branches.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


}