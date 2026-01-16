package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.review.CreateReviewDto;
import com.hszadkowski.iwa_backend.dto.review.ReviewResponseDto;
import com.hszadkowski.iwa_backend.services.interfaces.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponseDto> createReview(
            @RequestPart("review") @Valid CreateReviewDto reviewDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {

        String userEmail = authentication.getName();
        return ResponseEntity.ok(reviewService.createReview(userEmail, reviewDto, files));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewsForService(@PathVariable Integer serviceId) {
        return ResponseEntity.ok(reviewService.getReviewsForService(serviceId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponseDto>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Integer reviewId,
            Authentication authentication) {

        String userEmail = authentication.getName();
        reviewService.deleteReview(reviewId, userEmail);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Integer reviewId,
            @RequestPart("review") @Valid CreateReviewDto reviewDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {

        String userEmail = authentication.getName();
        return ResponseEntity.ok(reviewService.updateReview(reviewId, userEmail, reviewDto, files));
    }
}