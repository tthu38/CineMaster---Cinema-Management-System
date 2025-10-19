package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.ContactRequestRequest;
import com.example.cinemaster.dto.response.ContactRequestResponse;
import com.example.cinemaster.entity.ContactRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ContactRequestMapper {

    @Mapping(target = "branch", ignore = true) // gán trong service
    @Mapping(target = "handledBy", ignore = true)
    ContactRequest toEntity(ContactRequestRequest dto);

    @Mapping(source = "handledBy.fullName", target = "handledBy", defaultValue = "Chưa xử lý")
    @Mapping(source = "branch.branchName", target = "branchName", defaultValue = "Không xác định")
    @Mapping(source = "handleNote", target = "handleNote")
    @Mapping(source = "handledAt", target = "handledAt")
    ContactRequestResponse toResponse(ContactRequest entity);
}
