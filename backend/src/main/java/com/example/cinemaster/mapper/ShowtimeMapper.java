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

    @Mapping(target = "periodId", source = "period.id")
    @Mapping(target = "auditoriumId", source = "auditorium.auditoriumID")
    @Mapping(target = "movieTitle", source = "period.movie.title")
    @Mapping(target = "auditoriumName", source = "auditorium.name")
    @Mapping(target = "branchId", source = "auditorium.branch.id")

    ShowtimeResponse toResponse(Showtime entity);

    List<ShowtimeResponse> toResponseList(List<Showtime> entities);

    @Mapping(target = "showtimeID", ignore = true)
    Showtime toEntity(ShowtimeCreateRequest dto, ScreeningPeriod period, Auditorium auditorium);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ShowtimeUpdateRequest dto,
                                 @MappingTarget Showtime entity,
                                 @Context ScreeningPeriod period,
                                 @Context Auditorium auditorium);
}
