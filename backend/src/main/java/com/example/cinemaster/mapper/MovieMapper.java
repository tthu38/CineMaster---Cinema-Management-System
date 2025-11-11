package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.MovieRequest;
import com.example.cinemaster.dto.response.MovieResponse;
import com.example.cinemaster.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MovieMapper {

    Movie toEntity(MovieRequest request);

    void updateEntity(@MappingTarget Movie movie, MovieRequest request);

    @Mapping(source = "movieID", target = "movieId")
    MovieResponse toMovieResponse(Movie movie);

    List<MovieResponse> toMovieResponseList(List<Movie> movies);
}