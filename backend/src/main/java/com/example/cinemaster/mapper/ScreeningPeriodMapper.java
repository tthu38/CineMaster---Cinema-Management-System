// src/main/java/com/example/cinemaster/mapper/ScreeningPeriodMapper.java
package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.ScreeningPeriodResponse;
import com.example.cinemaster.entity.ScreeningPeriod;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScreeningPeriodMapper {

    @Mappings({
            @Mapping(target = "periodId",  source = "id"),
            @Mapping(target = "movieId",   source = "movie.movieID"),
            @Mapping(target = "movieTitle",source = "movie.title"),
            @Mapping(target = "branchId",  source = "branch.id"),
            @Mapping(target = "startDate", source = "startDate"),
            @Mapping(target = "endDate",   source = "endDate"),
            @Mapping(target = "duration",  source = "movie.duration")
    })
    ScreeningPeriodResponse toLite(ScreeningPeriod entity);

    List<ScreeningPeriodResponse> toLiteList(List<ScreeningPeriod> list);
}
