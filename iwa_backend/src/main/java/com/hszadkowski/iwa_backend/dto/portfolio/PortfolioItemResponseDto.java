package com.hszadkowski.iwa_backend.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItemResponseDto {
    private Integer id;
    private List<String> imageUrls;
    private String description;
    private String category;
    private LocalDate createdAt;
}