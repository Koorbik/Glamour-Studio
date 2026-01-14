package com.hszadkowski.iwa_backend.dto.review;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ReviewResponseDto {
    Integer reviewId;
    String authorName;
    Integer rating;
    String comment;
    LocalDate createdAt;
}
