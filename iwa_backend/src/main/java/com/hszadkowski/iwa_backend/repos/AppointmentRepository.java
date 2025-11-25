package com.hszadkowski.iwa_backend.repos;

import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AppointmentStatus;
import com.hszadkowski.iwa_backend.models.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByAppUser(AppUser appUser);

    List<Appointment> findByScheduledAtAndStatus(LocalDate scheduledAt, AppointmentStatus status);

    List<Appointment> findByScheduledAt(LocalDate scheduledAt);

    List<Appointment> findByAppUserAndScheduledAtAfterAndStatusNameNot(
            AppUser user,
            LocalDate date,
            String statusName
    );
    
    Optional<Appointment> findBySlotAndStatusNameNotIn(AvailabilitySlot slot, List<String> excludedStatuses);

    @Query("SELECT a FROM Appointment a WHERE a.status.name = 'CONFIRMED' AND a.createdAt < :threshold AND (a.payment IS NULL OR a.payment.status != 'COMPLETED')")
    List<Appointment> findUnpaidConfirmedAppointmentsOlderThan(@Param("threshold") LocalDateTime threshold);
}
