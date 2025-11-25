package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.auth.GoogleAuthCodeDto;
import com.hszadkowski.iwa_backend.dto.calendar.CalendarTokenResponseDto;
import com.hszadkowski.iwa_backend.dto.calendar.GoogleCalendarEventDto;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    @GetMapping("/google/auth-url")
    public ResponseEntity<Map<String, String>> getGoogleAuthUrl(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String authUrl = googleCalendarService.getAuthorizationUrl(userEmail);
            return ResponseEntity.ok(Map.of("authUrl", authUrl));
        } catch (Exception e) {
            log.error("Error generating auth URL: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to generate authorization URL"));
        }
    }

    @PostMapping("/google/callback")
    public ResponseEntity<CalendarTokenResponseDto> handleGoogleCallback(
            @RequestBody GoogleAuthCodeDto authCodeDto,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            CalendarTokenResponseDto response = googleCalendarService.handleOAuthCallback(
                    authCodeDto.getAuthCode(), userEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error handling Google OAuth callback: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/google/status")
    public ResponseEntity<CalendarTokenResponseDto> getGoogleCalendarStatus(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            CalendarTokenResponseDto status = googleCalendarService.getCalendarConnectionStatus(userEmail);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting calendar status: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/google/disconnect")
    public ResponseEntity<Map<String, String>> disconnectGoogleCalendar(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            googleCalendarService.disconnectGoogleCalendar(userEmail);
            return ResponseEntity.ok(Map.of("message", "Successfully disconnected from Google Calendar"));
        } catch (Exception e) {
            log.error("Error disconnecting Google Calendar: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to disconnect from Google Calendar"));
        }
    }

    @GetMapping("/google/calendars")
    public ResponseEntity<List<GoogleCalendarEventDto>> getUserCalendars(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            if (!googleCalendarService.isUserConnectedToGoogleCalendar(userEmail)) {
                return ResponseEntity.badRequest().build();
            }

            List<GoogleCalendarEventDto> calendars = googleCalendarService.getUserCalendars(userEmail);
            return ResponseEntity.ok(calendars);
        } catch (Exception e) {
            log.error("Error getting user calendars: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/google/refresh")
    public ResponseEntity<Map<String, String>> refreshGoogleToken(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            googleCalendarService.refreshAccessTokenIfNeeded(userEmail);
            return ResponseEntity.ok(Map.of("message", "Token refreshed successfully"));
        } catch (Exception e) {
            log.error("Error refreshing Google token: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to refresh token"));
        }
    }

    @GetMapping("/sync-status/{appointmentId}")
    public ResponseEntity<Map<String, Object>> getAppointmentSyncStatus(
            @PathVariable Integer appointmentId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();

            boolean isSynced = googleCalendarService.isAppointmentSynced(appointmentId, userEmail);
            boolean isConnected = googleCalendarService.isUserConnectedToGoogleCalendar(userEmail);

            return ResponseEntity.ok(Map.of(
                    "isSynced", isSynced,
                    "isConnected", isConnected,
                    "appointmentId", appointmentId
            ));

        } catch (Exception e) {
            log.error("Error getting appointment sync status: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get sync status"));
        }
    }

    @PostMapping("/sync-existing-appointments")
    public ResponseEntity<Map<String, Object>> syncExistingAppointments(Authentication authentication) {
        try {
            String userEmail = authentication.getName();

            if (!googleCalendarService.isUserConnectedToGoogleCalendar(userEmail)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Not connected to Google Calendar"));
            }

            int syncedCount = googleCalendarService.syncExistingAppointments(userEmail);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully synced appointments",
                    "syncedCount", syncedCount
            ));

        } catch (Exception e) {
            log.error("Error syncing existing appointments: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to sync appointments"));
        }
    }
}