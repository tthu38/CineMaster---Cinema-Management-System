package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.AccountRequest;
import com.example.cinemaster.dto.response.AccountResponse;
import com.example.cinemaster.entity.Account;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountManageMapper {

    @Mapping(source = "role.id", target = "roleId")
    @Mapping(source = "role.roleName", target = "roleName")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "branch.branchName", target = "branchName")
    AccountResponse toResponse(Account entity);

    Account toEntity(AccountRequest dto);
}

