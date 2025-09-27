package com.example.cinemaster.service;

import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowtimeService {
    private final ShowtimeRepository showtimeRepository;

    public List<ShowtimeResponse> getTodayShowtimes() {
        return showtimeRepository.findTodayShowtimes();
    }
}
