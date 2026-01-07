package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.calendar.CalendarSyncStatusDto;
import com.hszadkowski.iwa_backend.dto.calendar.CalendarTokenResponseDto;
import com.hszadkowski.iwa_backend.dto.calendar.GoogleCalendarTokenDto;
import com.hszadkowski.iwa_backend.dto.user.UserProfileDto;
import com.hszadkowski.iwa_backend.dto.user.UserProfileUpdateDto;
import com.hszadkowski.iwa_backend.exceptions.UserAlreadyExistsException;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleCalendarService;
import com.hszadkowski.iwa_backend.services.interfaces.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final GoogleCalendarService googleCalendarService;

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Void> handleUserExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(303).location(URI.create("/user-already-exist")).build();
    }

    // just for testing
    @GetMapping("/me")
    public ResponseEntity<AppUser> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AppUser currentUser = (AppUser) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("/users/profile")
    public ResponseEntity<UserProfileDto> getUserProfile() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String userEmail = authentication.getName();
            System.out.println("Getting profile for user: " + userEmail);

            AppUser user = userService.findByEmail(userEmail);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            UserProfileDto profile = UserProfileDto.builder()
                    .name(user.getName())
                    .surname(user.getSurname())
                    .email(user.getEmail())
                    .phoneNum(user.getPhoneNum())
                    .smsNotificationsEnabled(user.isSmsNotificationsEnabled())
                    .build();

            System.out.println("Profile data: " + profile);

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            System.err.println("Error getting user profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/users/profile")
    public ResponseEntity<UserProfileDto> updateUserProfile(@Valid @RequestBody UserProfileUpdateDto updateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        AppUser updatedUser = userService.updateUserProfile(userEmail, updateDto);

        UserProfileDto profile = UserProfileDto.builder()
                .name(updatedUser.getName())
                .surname(updatedUser.getSurname())
                .email(updatedUser.getEmail())
                .phoneNum(updatedUser.getPhoneNum())
                .smsNotificationsEnabled(updatedUser.isSmsNotificationsEnabled())
                .build();

        return ResponseEntity.ok(profile);
    }

    // Google Calendar endpoints
    @PostMapping("/users/google-calendar/token")
    public ResponseEntity<CalendarTokenResponseDto> saveGoogleCalendarToken(
            @RequestBody GoogleCalendarTokenDto tokenDto,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            log.info("Saving Google Calendar token for user: {}", userEmail);
            
            // The frontend sends an access token directly from implicit flow
            // We need to save it and get user info
            CalendarTokenResponseDto response = googleCalendarService.saveAccessToken(
                    tokenDto.getAccessToken(), userEmail);
                    
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error saving Google Calendar token: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/users/google-calendar/status")
    public ResponseEntity<CalendarSyncStatusDto> getGoogleCalendarStatus(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            CalendarTokenResponseDto tokenStatus = googleCalendarService.getCalendarConnectionStatus(userEmail);
            
            CalendarSyncStatusDto status = new CalendarSyncStatusDto();
            status.setSynced(tokenStatus.isConnected());
            status.setCalendarId(tokenStatus.getEmail()); // Using email as calendar identifier
            status.setLastSyncTime(tokenStatus.getExpiresAt() != null ? 
                    tokenStatus.getExpiresAt().toString() : null);
            status.setSyncEnabled(tokenStatus.isConnected());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting Google Calendar status: ", e);
            
            // Return disconnected status on error
            CalendarSyncStatusDto status = new CalendarSyncStatusDto();
            status.setSynced(false);
            status.setSyncEnabled(false);
            
            return ResponseEntity.ok(status);
        }
    }

    @DeleteMapping("/users/google-calendar/sync")
    public ResponseEntity<Map<String, String>> disableGoogleCalendarSync(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            googleCalendarService.disconnectGoogleCalendar(userEmail);
            
            return ResponseEntity.ok(Map.of(
                    "message", "Successfully disconnected from Google Calendar"
            ));
        } catch (Exception e) {
            log.error("Error disabling Google Calendar sync: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to disconnect from Google Calendar"));
        }
    }
}
