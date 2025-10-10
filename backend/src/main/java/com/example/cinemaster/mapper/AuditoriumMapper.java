package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.response.AuditoriumResponse;
import com.example.cinemaster.entity.Auditorium;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditoriumMapper {

    @Mapping(source = "auditoriumID", target = "auditoriumID")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "capacity", target = "capacity")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "branch.branchName", target = "branchName")
    @Mapping(source = "isActive", target = "isActive")
    AuditoriumResponse toResponse(Auditorium entity);

    List<AuditoriumResponse> toResponseList(List<Auditorium> entities);
}
