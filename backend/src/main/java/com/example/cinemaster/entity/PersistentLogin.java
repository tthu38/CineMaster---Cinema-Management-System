package com.example.cinemaster.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "persistent_logins")
public class PersistentLogin {
    @Id
    @Size(max = 64)
    @Column(name = "series", nullable = false, length = 64)
    private String series;

    @Size(max = 64)
    @NotNull
    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Size(max = 64)
    @NotNull
    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @NotNull
    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;
}