package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.ScreeningPeriodRequest;
import com.example.cinemaster.dto.response.ScreeningPeriodResponse;
import com.example.cinemaster.entity.ScreeningPeriod;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScreeningPeriodMapper {

    @Mappings({
            @Mapping(target = "id",          source = "id"),
            @Mapping(target = "movieId",     source = "movie.movieID"),
            @Mapping(target = "movieTitle",  source = "movie.title"),
            @Mapping(target = "branchId",    source = "branch.id"),
            @Mapping(target = "branchName",  source = "branch.branchName"),
            @Mapping(target = "startDate",   source = "startDate"),
            @Mapping(target = "endDate",     source = "endDate"),
            @Mapping(target = "isActive",    source = "isActive")
    })
    ScreeningPeriodResponse toLite(ScreeningPeriod entity);

    List<ScreeningPeriodResponse> toLiteList(List<ScreeningPeriod> list);

    // ⚡ Dùng để cập nhật entity từ request
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ScreeningPeriodRequest dto, @MappingTarget ScreeningPeriod entity);
}
