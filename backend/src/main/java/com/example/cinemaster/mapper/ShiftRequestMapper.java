package com.example.cinemaster.mapper;


import com.example.cinemaster.dto.response.ShiftRequestResponse;
import com.example.cinemaster.entity.ShiftRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


import java.util.List;


@Mapper(componentModel = "spring")
public interface ShiftRequestMapper {


    @Mapping(source = "requestID", target = "requestID")
    @Mapping(source = "account.accountID", target = "accountId")
    @Mapping(source = "account.fullName", target = "accountName")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "branch.branchName", target = "branchName")
    @Mapping(source = "shiftDate", target = "shiftDate")
    @Mapping(source = "shiftType", target = "shiftType")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "note", target = "note")
    @Mapping(source = "createdAt", target = "createdAt")
    ShiftRequestResponse toResponse(ShiftRequest sr);


    List<ShiftRequestResponse> toResponseList(List<ShiftRequest> list);
}



