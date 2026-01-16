package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.review.CreateReviewDto;
import com.hszadkowski.iwa_backend.dto.review.ReviewResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReviewService {
    ReviewResponseDto createReview(String userEmail, CreateReviewDto dto, MultipartFile file);
    List<ReviewResponseDto> getReviewsForService(Integer serviceId);
}
