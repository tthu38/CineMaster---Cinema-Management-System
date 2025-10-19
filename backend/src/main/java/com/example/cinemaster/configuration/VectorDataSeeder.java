package com.example.cinemaster.configuration;

import com.example.cinemaster.entity.*;
import com.example.cinemaster.repository.*;
import com.example.cinemaster.service.VectorStoreService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VectorDataSeeder {

    private final VectorStoreService vectorStore;
    private final MovieRepository movieRepo;
    private final DiscountRepository discountRepo;
    private final ComboRepository comboRepo;
    private final BranchRepository branchRepo;
    private final MembershipLevelRepository membershipLevelRepo;

    @PostConstruct
    public void seed() {
        try {
            ingestMovies();
            ingestCombos();
            ingestDiscounts();
            ingestBranches();
            ingestMembershipLevels();
            System.out.println("✅ VectorDataSeeder: Seed thành công vào VectorStore.");
        } catch (Exception e) {
            System.err.println("❌ VectorDataSeeder lỗi: " + e.getMessage());
        }
    }

    private void ingestMovies() {
        List<Movie> movies = movieRepo.findAll();
        if (movies.isEmpty()) return;

        List<String> docs = movies.stream().map(m -> """
            [MOVIE]
            Tên phim: %s
            Đạo diễn: %s
            Diễn viên: %s
            Thể loại: %s
            Thời lượng: %s phút
            Quốc gia: %s
            Trạng thái: %s
            Mô tả: %s
            Ngày khởi chiếu: %s
            """.formatted(
                safe(m.getTitle()), safe(m.getDirector()), safe(m.getCast()),
                safe(m.getGenre()), safe(m.getDuration()),
                safe(m.getCountry()), safe(m.getStatus()),
                safe(m.getDescription()), safe(m.getReleaseDate())
        )).collect(Collectors.toCollection(ArrayList::new));

        vectorStore.ingestDocuments("movies", docs);
    }

    private void ingestCombos() {
        List<Combo> combos = comboRepo.findAll();
        if (combos.isEmpty()) return;

        List<String> docs = combos.stream()
                .map(c -> """
            [COMBO]
            Tên combo: %s
            Giá: %s
            Thành phần: %s
            Mô tả: %s
            Đang bán: %s
            """.formatted(
                        safe(c.getNameCombo()),
                        safe(c.getPrice()),
                        safe(c.getItems()),
                        safe(c.getDescriptionCombo()),
                        safe(c.getAvailable())
                ))
                .toList();

        vectorStore.ingestDocuments("combos", docs);
    }
    private void ingestDiscounts() {
        List<Discount> list = discountRepo.findAll();
        if (list.isEmpty()) return;

        List<String> docs = list.stream().map(d -> """
            [DISCOUNT]
            Mã: %s
            Trạng thái: %s
            Miêu tả: %s
            Giảm phần trăm: %s
            Giảm tiền cố định: %s
            Điểm quy đổi: %s
            Ngày tạo: %s
            Hạn dùng: %s
            """.formatted(
                safe(d.getCode()), safe(d.getDiscountStatus()),
                safe(d.getDiscountDescription()),
                safe(d.getPercentOff()), safe(d.getFixedAmount()), safe(d.getPointCost()),
                safe(d.getCreateAt()), safe(d.getExpiryDate())
        )).toList();

        vectorStore.ingestDocuments("discounts", docs);
    }

    private void ingestBranches() {
        List<Branch> branches = branchRepo.findAll();
        if (branches.isEmpty()) return;

        List<String> docs = branches.stream().map(b -> """
            [BRANCH]
            Chi nhánh: %s
            Địa chỉ: %s
            Điện thoại: %s
            Email: %s
            Giờ mở cửa: %s
            Giờ đóng cửa: %s
            Trạng thái: %s
            """.formatted(
                safe(b.getBranchName()), safe(b.getAddress()), safe(b.getPhone()),
                safe(b.getEmail()), safe(b.getOpenTime()), safe(b.getCloseTime()), safe(b.getIsActive())
        )).toList();

        vectorStore.ingestDocuments("branches", docs);
    }

    private void ingestMembershipLevels() {
        List<MembershipLevel> levels = membershipLevelRepo.findAll();
        if (levels.isEmpty()) return;

        List<String> docs = levels.stream().map(lv -> """
            [MEMBERSHIP]
            Cấp bậc: %s
            Điểm tối thiểu: %s
            Điểm tối đa: %s
            Quyền lợi: %s
            """.formatted(
                safe(lv.getLevelName()), safe(lv.getMinPoints()), safe(lv.getMaxPoints()), safe(lv.getBenefits())
        )).toList();

        vectorStore.ingestDocuments("membership", docs);
    }

    private String safe(Object v) {
        return v == null ? "N/A" : String.valueOf(v);
    }
}
