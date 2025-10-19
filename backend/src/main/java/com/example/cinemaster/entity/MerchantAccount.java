package com.example.cinemaster.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
public class MerchantAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 20)
    @NotNull
    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;

    @Size(max = 50)
    @NotNull
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Size(max = 200)
    @NotNull
    @Column(name = "account_name", nullable = false, length = 200)
    private String accountName;

    @ColumnDefault("0")
    @Column(name = "is_default")
    private Integer isDefault;

    @ColumnDefault("getdate()")
    @Column(name = "created_at")
    private Instant createdAt;

}