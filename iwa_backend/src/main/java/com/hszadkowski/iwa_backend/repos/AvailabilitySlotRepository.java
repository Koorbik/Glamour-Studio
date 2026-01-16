package com.hszadkowski.iwa_backend.repos;

import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.models.AvailabilitySlot;
import com.hszadkowski.iwa_backend.models.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Integer> {

    List<AvailabilitySlot> findByService(Service service);

    List<AvailabilitySlot> findByIsBookedFalseAndStartTimeBetween(
            LocalDateTime startTime, LocalDateTime endTime);

    List<AvailabilitySlot> findByServiceAndIsBookedFalseAndStartTimeBetweenOrderByStartTimeAsc(
            Service service, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.appUser = :admin AND " +
            "((a.startTime <= :startTime AND a.endTime > :startTime) OR " +
            "(a.startTime < :endTime AND a.endTime >= :endTime) OR " +
            "(a.startTime >= :startTime AND a.endTime <= :endTime))")
    List<AvailabilitySlot> findOverlappingSlots(@Param("admin") AppUser admin,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.appUser = :admin AND " +
            "a.slotId != :excludeId AND " +
            "((a.startTime <= :startTime AND a.endTime > :startTime) OR " +
            "(a.startTime < :endTime AND a.endTime >= :endTime) OR " +
            "(a.startTime >= :startTime AND a.endTime <= :endTime))")
    List<AvailabilitySlot> findOverlappingSlotsExcluding(@Param("admin") AppUser admin,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime,
                                                         @Param("excludeId") Integer excludeId);

}
