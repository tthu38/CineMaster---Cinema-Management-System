package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.BranchNameResponse;
import com.example.cinemaster.entity.Branch;
import com.example.cinemaster.repository.BranchRepository;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchRepository branchRepository;

    @GetMapping("/names")
    public List<BranchNameResponse> getBranchNames() {
        return branchRepository.findAllBranchNames();
    }
}

