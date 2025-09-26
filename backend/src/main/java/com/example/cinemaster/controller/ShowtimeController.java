package com.example.cinemaster.controller;

import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth") // 👈 nhớ có /auth như bạn yêu cầu
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    // API public để lấy suất chiếu hôm nay
    @GetMapping("/showtimes/today")
    public List<ShowtimeResponse> getTodayShowtimes() {
        return showtimeService.getTodayShowtimes();
    }
}
