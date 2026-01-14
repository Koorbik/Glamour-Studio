package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.review.CreateReviewDto;
import com.hszadkowski.iwa_backend.dto.review.ReviewResponseDto;
import com.hszadkowski.iwa_backend.services.interfaces.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponseDto> createReview(
            @RequestBody @Valid CreateReviewDto reviewDto,
            Authentication authentication) {

        String userEmail = authentication.getName();
        return ResponseEntity.ok(reviewService.createReview(userEmail, reviewDto));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsForService(@PathVariable Integer serviceId) {
        return ResponseEntity.ok(reviewService.getReviewsForService(serviceId));
    }
}