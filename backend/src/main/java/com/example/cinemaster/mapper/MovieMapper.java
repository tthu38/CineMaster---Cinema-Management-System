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

    // Request -> Entity (khi tạo mới)
    Movie toEntity(MovieRequest request);

    // Cập nhật Entity từ Request (dùng cho update)
    void updateEntity(@MappingTarget Movie movie, MovieRequest request);

    // Entity -> Response (Chỉ giữ lại toMovieResponse và map các trường khác tên)
    // MapStruct tự động map các trường trùng tên (title, genre, cast, description, v.v.)
    @Mapping(source = "movieID", target = "movieId")
    MovieResponse toMovieResponse(Movie movie);

    // List Entity -> List Response
    // MapStruct tự động sử dụng toMovieResponse() để triển khai
    List<MovieResponse> toMovieResponseList(List<Movie> movies);
}