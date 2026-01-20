package com.hszadkowski.iwa_backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "portfolio_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ElementCollection
    @CollectionTable(name = "portfolio_images", joinColumns = @JoinColumn(name = "portfolio_item_id"))
    @Column(name = "image_url")
    private List<String> imageUrls = new ArrayList<>();

    @Column(length = 500)
    private String description;

    private String category;

    @Column(name = "created_at")
    private LocalDate createdAt;

    // Auto-set the date before persisting if not provided
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }
}