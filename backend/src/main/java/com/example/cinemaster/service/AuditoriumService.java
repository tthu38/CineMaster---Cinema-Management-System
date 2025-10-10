package com.example.cinemaster.service;

import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.mapper.AuditoriumMapper;
import com.example.cinemaster.repository.AuditoriumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditoriumService {

    private final AuditoriumRepository repo;
    private final AuditoriumMapper mapper;

    public List<AuditoriumResponse> listByBranch(Integer branchId) {
        List<Auditorium> list = (branchId == null)
                ? repo.findAll()
                : repo.findActiveByBranch(branchId);
        return mapper.toLiteList(list);
    }
}
