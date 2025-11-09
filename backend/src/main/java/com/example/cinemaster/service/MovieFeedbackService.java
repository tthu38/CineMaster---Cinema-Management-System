package com.example.cinemaster.service;

import com.example.cinemaster.dto.request.MovieFeedbackRequest;
import com.example.cinemaster.dto.response.MovieFeedbackResponse;
import com.example.cinemaster.entity.Account;
import com.example.cinemaster.entity.Movie;
import com.example.cinemaster.entity.MovieFeedback;
import com.example.cinemaster.repository.AccountRepository;
import com.example.cinemaster.repository.MovieFeedbackRepository;
import com.example.cinemaster.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieFeedbackService {

    private final MovieFeedbackRepository feedbackRepo;
    private final MovieRepository movieRepo;
    private final AccountRepository accountRepo;

    public List<MovieFeedbackResponse> getByMovie(Integer movieId) {
        return feedbackRepo.findByMovie_MovieID(movieId)
                .stream()
                .map(f -> MovieFeedbackResponse.builder()
                        .id(f.getFeedbackId())
                        .accountId(f.getAccount().getAccountID()) // 👈 thêm
                        .accountName(f.getAccount().getFullName())
                        .rating(f.getRating())
                        .comment(f.getComment())
                        .createdAt(f.getCreatedAt())
                        .build())
                .toList();
    }

    public MovieFeedbackResponse create(Integer movieId, MovieFeedbackRequest req) {
        Movie movie = movieRepo.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        Account acc = accountRepo.findById(req.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        MovieFeedback fb = new MovieFeedback();
        fb.setMovie(movie);
        fb.setAccount(acc);
        fb.setRating(req.getRating());
        fb.setComment(req.getComment());
        fb.setCreatedAt(Instant.now());

        MovieFeedback saved = feedbackRepo.save(fb);

        return MovieFeedbackResponse.builder()
                .id(saved.getFeedbackId())
                .accountId(acc.getAccountID()) // 👈 thêm
                .accountName(acc.getFullName())
                .rating(saved.getRating())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public void delete(Integer id, Integer accountId) {
        MovieFeedback f = feedbackRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        if (!f.getAccount().getAccountID().equals(accountId)) {
            throw new RuntimeException("Bạn không có quyền xóa feedback này!");
        }
        feedbackRepo.delete(f);
    }

    public MovieFeedbackResponse update(Integer id, MovieFeedbackRequest req) {
        MovieFeedback feedback = feedbackRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));
        if (!feedback.getAccount().getAccountID().equals(req.getAccountId())) {
            throw new RuntimeException("Bạn không có quyền sửa feedback này");
        }

        feedback.setRating(req.getRating());
        feedback.setComment(req.getComment());
        feedbackRepo.save(feedback);

        return MovieFeedbackResponse.builder()
                .id(feedback.getFeedbackId())
                .accountId(feedback.getAccount().getAccountID()) // 👈 thêm
                .accountName(feedback.getAccount().getFullName())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    public List<MovieFeedback> getAllFeedbacksByUser(Integer accountId) {
        return feedbackRepo.findByAccount_AccountID(accountId);
    }
}
