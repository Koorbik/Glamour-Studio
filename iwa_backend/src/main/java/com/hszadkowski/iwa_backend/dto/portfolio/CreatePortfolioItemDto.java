package com.hszadkowski.iwa_backend.dto.portfolio;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CreatePortfolioItemDto {

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 50, message = "Category name is too long")
    private String category;

    private List<String> retainedImageUrls;
}