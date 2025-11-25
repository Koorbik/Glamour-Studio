package com.hszadkowski.iwa_backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Integer appointmentId;

    @ManyToOne
    @JoinColumn(name = "slot_id")
    private AvailabilitySlot slot;

    @ManyToOne
    @JoinColumn(name = "app_user_id")
    private AppUser appUser;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private Service service;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private AppointmentStatus status;

    private String location;
    private LocalDate scheduledAt;
    private String description;

    @OneToOne(mappedBy = "appointment")
    private Payment payment;

    @OneToOne(mappedBy = "appointment")
    private Review review;

    @OneToMany(mappedBy = "appointment")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalendarEvent> calendarEvents;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}