package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.SeatTypeRequest;
import com.example.cinemaster.dto.response.SeatTypeResponse;
import com.example.cinemaster.entity.SeatType;
import com.example.cinemaster.repository.SeatTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatTypeService {

    private final SeatTypeRepository seatTypeRepository;

    public SeatTypeService(SeatTypeRepository seatTypeRepository) {
        this.seatTypeRepository = seatTypeRepository;
    }

    private SeatTypeResponse mapToResponse(SeatType seatType) {
        return SeatTypeResponse.builder()
                .typeID(seatType.getTypeID())
                .typeName(seatType.getTypeName())
                .priceMultiplier(seatType.getPriceMultiplier())
                .build();
    }

    public List<SeatTypeResponse> getAllSeatTypes() {
        return seatTypeRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SeatTypeResponse createSeatType(SeatTypeRequest request) {
        SeatType seatType = SeatType.builder()
                .typeName(request.getTypeName())
                .priceMultiplier(request.getPriceMultiplier())
                .build();
        SeatType saved = seatTypeRepository.save(seatType);
        return mapToResponse(saved);
    }

}
