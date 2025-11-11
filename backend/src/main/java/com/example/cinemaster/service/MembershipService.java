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

    /**  Cập nhật hạng & điểm của user sau khi thanh toán */
    @Transactional
    public void updateMembershipAfterPayment(Account account, int earnedPoints) {
        if (account == null) return;

        Membership membership = membershipRepository.findByAccount(account)
                .orElseGet(() -> {
                    Membership m = new Membership();
                    m.setAccount(account);
                    m.setPoints(0);
                    m.setJoinDate(LocalDate.now());
                    return m;
                });

        membership.setPoints(membership.getPoints() + earnedPoints);

        MembershipLevel newLevel = levelRepository.findAll().stream()
                .filter(lv -> membership.getPoints() >= lv.getMinPoints() && membership.getPoints() <= lv.getMaxPoints())
                .findFirst()
                .orElse(null);

        if (newLevel != null && (membership.getLevel() == null ||
                !newLevel.getId().equals(membership.getLevel().getId()))) {
            membership.setLevel(newLevel);
        }

        membership.setExpiryDate(LocalDate.now().plusYears(1));
        membershipRepository.save(membership);
    }
}
