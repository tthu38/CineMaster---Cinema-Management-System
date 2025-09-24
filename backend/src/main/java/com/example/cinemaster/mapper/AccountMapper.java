package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.UpdateProfileRequest;
import com.example.cinemaster.dto.response.ProfileResponse;
import com.example.cinemaster.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    // Entity -> DTO
    @Mapping(source = "accountID", target = "id")
    @Mapping(source = "role.roleName", target = "roleName")
    ProfileResponse toProfileResponse(Account acc);

    // Request -> Entity (update trực tiếp vào entity đang có)
    void updateAccount(@MappingTarget Account account, UpdateProfileRequest request);
}
