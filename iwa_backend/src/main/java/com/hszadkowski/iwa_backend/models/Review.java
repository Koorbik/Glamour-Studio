package com.hszadkowski.iwa_backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @OneToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    private Integer rating;

    @Column(length = 1000)
    private String comment;

    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "created_at")
    private LocalDate createdAt;
}