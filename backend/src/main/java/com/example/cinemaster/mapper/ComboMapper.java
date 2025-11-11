package com.example.cinemaster.mapper;

import com.example.cinemaster.dto.request.ComboRequest;
import com.example.cinemaster.dto.response.ComboResponse;
import com.example.cinemaster.entity.Combo;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComboMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branchID", ignore = true) // sẽ được gán thủ công trong service
    Combo toEntity(ComboRequest request);

    @Mapping(source = "branchID.id", target = "branchId")
    @Mapping(source = "branchID.branchName", target = "branchName")
    ComboResponse toResponse(Combo combo);

    List<ComboResponse> toResponseList(List<Combo> combos);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branchID", ignore = true)
    @Mapping(target = "imageURL", ignore = true)
    void updateComboFromRequest(ComboRequest request, @MappingTarget Combo combo);
}
