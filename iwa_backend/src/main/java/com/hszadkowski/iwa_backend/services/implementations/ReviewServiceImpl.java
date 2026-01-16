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
import com.hszadkowski.iwa_backend.services.FileStorageService;
import com.hszadkowski.iwa_backend.services.interfaces.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReviewRepository reviewRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ReviewResponseDto createReview(String userEmail, CreateReviewDto dto, List<MultipartFile> files) {
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

        List<String> uploadedUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                        throw new RuntimeException("Only image files are allowed.");
                    }

                    String fileName = fileStorageService.storeFile(file);

                    String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/uploads/")
                            .path(fileName)
                            .toUriString();

                    uploadedUrls.add(fileUrl);
                }
            }
        }
        review.setAttachmentUrls(uploadedUrls);

        Review savedReview = reviewRepository.save(review);
        return mapToDto(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getReviewsForService(Integer serviceId) {
        List<Review> reviews = reviewRepository.findAllByServiceId(serviceId);
        return reviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (review.getAttachmentUrls() != null && !review.getAttachmentUrls().isEmpty()) {
            for (String url : review.getAttachmentUrls()) {
                fileStorageService.deleteFile(url);
            }
        }

        Appointment appointment = review.getAppointment();
        if (appointment != null) {
            appointment.setReview(null);
        }

        reviewRepository.delete(review);
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
                .attachmentUrls(review.getAttachmentUrls())
                .build();
    }
}