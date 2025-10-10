package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.MembershipLevelRequest;
import com.example.cinemaster.dto.response.MembershipLevelResponse;
import com.example.cinemaster.entity.MembershipLevel;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MembershipLevelMapper {

    MembershipLevel toEntity(MembershipLevelRequest request);

    MembershipLevelResponse toResponse(MembershipLevel entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget MembershipLevel target, MembershipLevelRequest source);
}
