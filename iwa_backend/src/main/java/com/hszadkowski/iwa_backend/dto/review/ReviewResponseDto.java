package com.hszadkowski.iwa_backend.dto.review;

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
public class ReviewResponseDto {
    private Integer reviewId;
    private String authorName;
    private Integer rating;
    private String comment;
    private LocalDate createdAt;
    private List<String> attachmentUrls;
}