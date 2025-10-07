package com.example.cinemaster.repository;

import com.example.cinemaster.entity.ScreeningPeriod;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreeningPeriodRepository extends JpaRepository<ScreeningPeriod, Integer> {

    /**
     * Sửa lỗi: Tên thuộc tính ID trong Entity Branch là 'id', nên phải dùng **findByBranch_Id**.
     * Tối ưu hóa: Dùng @EntityGraph để EAGER load (tải ngay lập tức) các quan hệ 'movie' và 'branch'
     * trong một truy vấn duy nhất.
     */

    // 1. Tối ưu hóa cho phương thức findAll() cơ bản (chống N+1 Selects)
    @EntityGraph(attributePaths = {"movie", "branch"})
    @Override
    List<ScreeningPeriod> findAll();

    // 2. Tìm kiếm theo BranchID và tối ưu hóa (Sửa lỗi cú pháp)
    // Tên phương thức được đổi từ findByBranch_BranchID thành **findByBranch_Id**
    @EntityGraph(attributePaths = {"movie", "branch"})
    List<ScreeningPeriod> findByBranch_Id(Integer branchId);

    // Bạn có thể thêm các tùy chỉnh khác nếu cần, ví dụ:
    // boolean existsByMovie_MovieIDAndBranch_Id(Integer movieID, Integer branchID);
}
