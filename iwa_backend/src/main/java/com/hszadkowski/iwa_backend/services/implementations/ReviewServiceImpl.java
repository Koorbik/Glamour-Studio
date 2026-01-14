package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.review.CreateReviewDto;
import com.hszadkowski.iwa_backend.dto.review.ReviewResponseDto;
import com.hszadkowski.iwa_backend.exceptions.AppointmentNotFoundException;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.Review;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.ReviewRepository;
import com.hszadkowski.iwa_backend.repos.UserRepository;
import com.hszadkowski.iwa_backend.services.interfaces.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public ReviewResponseDto createReview(String userEmail, CreateReviewDto dto) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Appointment appointment = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment with this ID does not exist"));

        validateReviewCreation(user, appointment);

        Review review = new Review();
        review.setAppointment(appointment);
        review.setAppUser(user);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setCreatedAt(LocalDate.now());

        Review savedReview = reviewRepository.save(review);

        return mapToDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsForService(Integer serviceId) {
        List<Review> reviews = reviewRepository.findAllByServiceId(serviceId);

        return reviews.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void validateReviewCreation(AppUser user, Appointment appointment) {
        if (!appointment.getAppUser().getAppUserId().equals(user.getAppUserId())) {
            throw new RuntimeException("You can only review your own appointments.");
        }

        if (appointment.getStatus() == null || !appointment.getStatus().getName().equalsIgnoreCase("COMPLETED")) {
            throw new RuntimeException("You can only review completed appointments.");
        }

        if (appointment.getReview() != null) {
            throw new RuntimeException("You have already reviewed this appointment.");
        }
    }
    private ReviewResponseDto mapToDto(Review review) {
        // Mask the last name for privacy (e.g., "Anna S.")
        String authorName = review.getAppUser().getName();
        if (review.getAppUser().getSurname() != null && !review.getAppUser().getSurname().isEmpty()) {
            authorName += " " + review.getAppUser().getSurname().charAt(0) + ".";
        }

        return ReviewResponseDto.builder()
                .reviewId(review.getReviewId())
                .authorName(authorName)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}