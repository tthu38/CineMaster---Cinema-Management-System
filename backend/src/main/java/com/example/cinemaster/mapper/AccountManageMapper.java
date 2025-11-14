package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.AccountRequest;
import com.example.cinemaster.dto.response.AccountResponse;
import com.example.cinemaster.entity.Account;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface AccountManageMapper {

    @Mapping(target = "roleId",
            expression = "java(entity.getRole() != null ? entity.getRole().getId() : null)")
    @Mapping(target = "roleName",
            expression = "java(entity.getRole() != null ? entity.getRole().getRoleName() : null)")
    @Mapping(target = "branchId",
            expression = "java(entity.getBranch() != null ? entity.getBranch().getId() : null)")
    @Mapping(target = "branchName",
            expression = "java(entity.getBranch() != null ? entity.getBranch().getBranchName() : null)")
    AccountResponse toResponse(Account entity);

    Account toEntity(AccountRequest dto);
}

