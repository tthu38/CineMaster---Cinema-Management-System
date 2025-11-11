package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.ShowtimeCreateRequest;
import com.example.cinemaster.dto.request.ShowtimeUpdateRequest;
import com.example.cinemaster.dto.response.ShowtimeResponse;
import com.example.cinemaster.entity.Auditorium;
import com.example.cinemaster.entity.ScreeningPeriod;
import com.example.cinemaster.entity.Showtime;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShowtimeMapper {

    @Mapping(target = "showtimeId", source = "showtimeID")
    @Mapping(target = "periodId", source = "period.id")
    @Mapping(target = "auditoriumId", source = "auditorium.auditoriumID")
    @Mapping(target = "movieId", source = "period.movie.movieID")
    @Mapping(target = "movieTitle", source = "period.movie.title")
    @Mapping(target = "posterUrl", source = "period.movie.posterUrl")
    @Mapping(target = "auditoriumName", source = "auditorium.name")
    @Mapping(target = "branchId", source = "auditorium.branch.id")
    ShowtimeResponse toResponse(Showtime entity);

    List<ShowtimeResponse> toResponseList(List<Showtime> entities);

    @Mapping(target = "showtimeID", ignore = true)
    @Mapping(target = "period", expression = "java(period)")
    @Mapping(target = "auditorium", expression = "java(auditorium)")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "language", source = "language")
    Showtime toEntity(ShowtimeCreateRequest dto,
                      @Context ScreeningPeriod period,
                      @Context Auditorium auditorium);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "period", expression = "java(period)")
    @Mapping(target = "auditorium", expression = "java(auditorium)")
    void updateEntityFromRequest(ShowtimeUpdateRequest dto,
                                 @MappingTarget Showtime entity,
                                 @Context ScreeningPeriod period,
                                 @Context Auditorium auditorium);

    @AfterMapping
    default void setDefaultStatus(@MappingTarget Showtime entity) {
        if (entity.getStatus() == null) {
            entity.setStatus("ACTIVE");
        }
    }
}
