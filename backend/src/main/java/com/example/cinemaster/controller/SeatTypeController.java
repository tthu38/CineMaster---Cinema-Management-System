package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.SeatTypeResponse;
import com.example.cinemaster.service.SeatTypeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seattypes")
public class SeatTypeController {

    private final SeatTypeService seatTypeService;

    public SeatTypeController(SeatTypeService seatTypeService) {
        this.seatTypeService = seatTypeService;
    }
    @GetMapping
    public List<SeatTypeResponse> getAllSeatTypes() {
        return seatTypeService.getAllSeatTypes();
    }

}
