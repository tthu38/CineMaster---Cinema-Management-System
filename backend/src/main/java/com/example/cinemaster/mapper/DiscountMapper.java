package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.DiscountRequest;
import com.example.cinemaster.dto.response.DiscountResponse;
import com.example.cinemaster.entity.Discount;
import com.example.cinemaster.entity.MembershipLevel;
import org.mapstruct.*;
import java.time.LocalDate;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface DiscountMapper {

    // 🧩 Map từ request sang entity (bao gồm requiredLevelId)
    @Mapping(target = "requiredLevel", expression = "java(toLevel(request.getRequiredLevelId()))")
    Discount toEntity(DiscountRequest request);

    // 🧩 Map từ entity sang response (hiển thị tên hạng & minOrderAmount)
    @Mapping(target = "requiredLevelName", source = "requiredLevel.levelName")
    DiscountResponse toResponse(Discount entity);

    // 🧩 Map cập nhật từ request sang entity (update case)
    @Mapping(target = "requiredLevel", expression = "java(toLevel(request.getRequiredLevelId()))")
    void updateDiscountFromRequest(DiscountRequest request, @MappingTarget Discount discount);

    // 🧩 Helper: chuyển ID thành MembershipLevel entity (để tránh query thủ công)
    default MembershipLevel toLevel(Integer id) {
        if (id == null) return null;
        MembershipLevel level = new MembershipLevel();
        level.setId(id);
        return level;
    }

    // 🧩 Set mặc định khi tạo mới
    @AfterMapping
    default void setDefaultValues(@MappingTarget Discount discount) {
        if (discount.getCreateAt() == null) {
            discount.setCreateAt(LocalDate.now());
        }
        if (discount.getDiscountStatus() == null) {
            discount.setDiscountStatus(Discount.DiscountStatus.ACTIVE); // ✅ dùng Enum thay vì String
        }
    }
}
