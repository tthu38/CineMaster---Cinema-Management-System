package com.example.cinemaster.entity;

import com.example.cinemaster.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "Discount")
@Table(schema = "dbo")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DiscountID", nullable = false)
    Integer discountID;

    @Size(max = 50)
    @Nationalized
    @Column(name = "Code", length = 50, unique = true)
    String code;

    @Nationalized
    @Lob
    @Column(name = "DiscountDescription")
    String discountDescription;

    @Column(name = "PercentOff", precision = 5, scale = 2)
    BigDecimal percentOff;

    @Column(name = "FixedAmount", precision = 10, scale = 2)
    BigDecimal fixedAmount;

    @Column(name = "PointCost")
    Integer pointCost;

    @Column(name = "MinOrderAmount", precision = 10, scale = 2)
    BigDecimal minOrderAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "RequiredLevelID", nullable = true)
    private MembershipLevel requiredLevel;

    @Column(name = "CreateAt")
    LocalDate createAt;

    @Column(name = "ExpiryDate")
    LocalDate expiryDate;

    @Column(name = "MaxUsage")
    Integer maxUsage;

    @Column(name = "MaxUsagePerAccount")
    Integer maxUsagePerAccount;

    @Column(name = "MaxUsagePerDay")
    Integer maxUsagePerDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "DiscountStatus", length = 20, nullable = false)
    @Builder.Default
    DiscountStatus discountStatus = DiscountStatus.ACTIVE;

    @Transient
    public DiscountType getDiscountType() {
        if (percentOff != null && percentOff.compareTo(BigDecimal.ZERO) > 0) {
            return DiscountType.PERCENT;
        } else if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return DiscountType.FIXED;
        }
        return null;
    }

    @Transient
    public BigDecimal getValue(BigDecimal basePrice) {
        if (basePrice == null) basePrice = BigDecimal.ZERO;

        BigDecimal discountValue = BigDecimal.ZERO;

        if (percentOff != null && percentOff.compareTo(BigDecimal.ZERO) > 0) {
            discountValue = basePrice
                    .multiply(percentOff)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        else if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) > 0) {
            discountValue = fixedAmount;
        }

        if (discountValue.compareTo(basePrice) > 0) {
            discountValue = basePrice;
        }

        return discountValue;
    }


    @Transient
    public String getDisplayText() {
        if (getDiscountType() == DiscountType.PERCENT) {
            return "Giảm " + percentOff.stripTrailingZeros().toPlainString() + "%";
        }
        if (getDiscountType() == DiscountType.FIXED) {
            return "Giảm " + fixedAmount.stripTrailingZeros().toPlainString() + "đ";
        }
        return "Không áp dụng giảm giá";
    }
    public enum DiscountStatus {
        ACTIVE,
        INACTIVE,
        EXPIRED,
        DELETED
    }
}
