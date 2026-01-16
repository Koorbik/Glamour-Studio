package com.hszadkowski.iwa_backend.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReviewDto {
    @NotNull
    private Integer appointmentId;

    @Min(1) @Max(5)
    private Integer rating;
    
    private String comment;
}
