package com.hszadkowski.iwa_backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendar_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Integer tokenId;

    @ManyToOne
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    private String provider;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private String email;
}