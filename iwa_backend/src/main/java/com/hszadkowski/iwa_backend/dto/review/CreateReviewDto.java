package com.hszadkowski.iwa_backend.dto.review;

import lombok.Data;

@Data
public class CreateReviewDto {
    Integer appointmentId;
    Integer rating;
    String comment;
}
