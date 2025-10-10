package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.MovieRequest;
import com.example.cinemaster.dto.response.MovieResponse;
import com.example.cinemaster.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MovieMapper {

    // Request -> Entity (khi tạo mới)
    Movie toEntity(MovieRequest request);

    // Cập nhật Entity từ Request (dùng cho update)
    void updateEntity(@MappingTarget Movie movie, MovieRequest request);

    // Entity -> Response (map rõ ràng tất cả field)
    @Mapping(source = "movieID", target = "movieId")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "genre", target = "genre")
    @Mapping(source = "duration", target = "duration")
    @Mapping(source = "releaseDate", target = "releaseDate")
    @Mapping(source = "director", target = "director")
    @Mapping(source = "cast", target = "cast")   // chú ý: entity phải là "cast" chứ không phải "casts"
    @Mapping(source = "description", target = "description")
    @Mapping(source = "language", target = "language")
    @Mapping(source = "ageRestriction", target = "ageRestriction")
    @Mapping(source = "country", target = "country")
    @Mapping(source = "trailerUrl", target = "trailerUrl")
    @Mapping(source = "posterUrl", target = "posterUrl")
    @Mapping(source = "status", target = "status")
    MovieResponse toResponse(Movie movie);
}
