package com.hszadkowski.iwa_backend.repos;

import com.hszadkowski.iwa_backend.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    @Query("SELECT r FROM Review r WHERE r.appointment.service.serviceId = :serviceId ORDER BY r.createdAt DESC")
    List<Review> findAllByServiceId(Integer serviceId);
}
