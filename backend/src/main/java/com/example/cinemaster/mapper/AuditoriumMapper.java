package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.entity.Auditorium;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditoriumMapper {

    @Mapping(source = "auditoriumID", target = "id")
    @Mapping(source = "name", target = "name")
    AuditoriumResponse toLite(Auditorium entity);

    List<AuditoriumResponse> toLiteList(List<Auditorium> entities);
}
