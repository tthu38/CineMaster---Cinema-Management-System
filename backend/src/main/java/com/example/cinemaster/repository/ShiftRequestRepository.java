package com.example.cinemaster.repository;


import com.example.cinemaster.entity.ShiftRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;


@Repository
public interface ShiftRequestRepository extends JpaRepository<ShiftRequest, Integer> {
    List<ShiftRequest> findByBranch_Id(Integer branchId);
    List<ShiftRequest> findByAccount_AccountID(Integer accountId);
    List<ShiftRequest> findByBranch_IdAndShiftDate(Integer branchId, LocalDate date);


    List<ShiftRequest> findByBranch_IdAndShiftDateBetween(Integer branchId,
                                                          LocalDate start,
                                                          LocalDate end);


}



