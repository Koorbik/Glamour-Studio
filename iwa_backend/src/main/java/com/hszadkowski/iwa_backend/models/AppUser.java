package com.hszadkowski.iwa_backend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_user_id")
    private Integer appUserId;

    private String name;
    private String surname;
    private String email;
    private String phoneNum;
    private String passwordHash;
    private String role;
    private String verificationCode;
    private LocalDateTime verificationCodeExpiresAt;
    private String passwordResetCode;
    private LocalDateTime passwordResetCodeExpiresAt;
    private boolean enabled;

    @Column(name = "sms_notifications_enabled")
    private boolean smsNotificationsEnabled = false;

    @OneToMany(mappedBy = "appUser")
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "appUser")
    private List<AvailabilitySlot> availabilitySlots;

    @OneToMany(mappedBy = "appUser")
    private List<Review> reviews;

    @OneToMany(mappedBy = "appUser")
    private List<Payment> payments;

    @OneToMany(mappedBy = "appUser")
    private List<Notification> notifications;

    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalendarToken> calendarTokens;

    @OneToMany(mappedBy = "appUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalendarEvent> calendarEvents;
}