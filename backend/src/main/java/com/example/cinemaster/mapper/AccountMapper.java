package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.RegisterRequest;
import com.example.cinemaster.entity.Account;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

@Mapper(componentModel = "spring", implementationName = "AccountMapperImpl")
public interface AccountMapper {
    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    // Tạo mới Account từ RegisterRequest
    @Mapping(target = "accountID", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "googleAuth", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "verificationCode", ignore = true)
    @Mapping(target = "verificationExpiry", ignore = true)
    // Nếu field ở request tên khác, mở comment dưới và chỉnh tên:
    // @Mapping(source = "accountAddress", target = "address")
    // @Mapping(source = "avatar", target = "avatarUrl")
    Account toEntity(RegisterRequest request);

    // Cập nhật Account sẵn có từ RegisterRequest
    @Mapping(target = "accountID", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "googleAuth", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "verificationCode", ignore = true)
    @Mapping(target = "verificationExpiry", ignore = true)
    void updateEntityFromRequest(RegisterRequest request, @MappingTarget Account account);

    // Tuỳ chọn: chuẩn hoá dữ liệu sau khi map
    @AfterMapping
    default void normalize(@MappingTarget Account account) {
        if (account.getEmail() != null) {
            account.setEmail(account.getEmail().trim().toLowerCase());
        }
        if (account.getPhoneNumber() != null) {
            account.setPhoneNumber(account.getPhoneNumber().trim());
        }
        // Nếu muốn set createdAt khi tạo mới (khi chưa có)
        if (account.getCreatedAt() == null) {
            account.setCreatedAt(LocalDate.now());
        }
    }
}
