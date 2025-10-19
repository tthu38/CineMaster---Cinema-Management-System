package com.example.cinemaster.service;

import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipLevelRepository levelRepository;

    /** 🔹 Cập nhật hạng & điểm của user sau khi thanh toán */
    @Transactional
    public void updateMembershipAfterPayment(Account account, int earnedPoints) {
        if (account == null) return;

        // 1️⃣ Lấy membership hiện tại
        Membership membership = membershipRepository.findByAccount(account)
                .orElseGet(() -> {
                    Membership m = new Membership();
                    m.setAccount(account);
                    m.setPoints(0);
                    m.setJoinDate(LocalDate.now());
                    return m;
                });

        // 2️⃣ Cộng thêm điểm
        membership.setPoints(membership.getPoints() + earnedPoints);

        // 3️⃣ Xác định cấp độ mới
        MembershipLevel newLevel = levelRepository.findAll().stream()
                .filter(lv -> membership.getPoints() >= lv.getMinPoints() && membership.getPoints() <= lv.getMaxPoints())
                .findFirst()
                .orElse(null);

        // 4️⃣ Gán cấp độ nếu khác
        if (newLevel != null && (membership.getLevel() == null ||
                !newLevel.getId().equals(membership.getLevel().getId()))) {
            membership.setLevel(newLevel);
        }

        // 5️⃣ Cập nhật ngày hết hạn (1 năm kể từ khi tham gia)
        membership.setExpiryDate(LocalDate.now().plusYears(1));

        membershipRepository.save(membership);
    }
}
