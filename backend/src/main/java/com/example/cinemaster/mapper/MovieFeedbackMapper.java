package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.MovieFeedbackResponse;
import com.example.cinemaster.entity.MovieFeedback;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MovieFeedbackMapper {
    MovieFeedbackResponse toResponse(MovieFeedback entity);
    List<MovieFeedbackResponse> toResponseList(List<MovieFeedback> list);
}
