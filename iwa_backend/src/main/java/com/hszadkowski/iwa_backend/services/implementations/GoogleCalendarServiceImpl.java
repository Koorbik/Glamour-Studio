package com.hszadkowski.iwa_backend.services.implementations;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import com.hszadkowski.iwa_backend.dto.calendar.CalendarTokenResponseDto;
import com.hszadkowski.iwa_backend.dto.calendar.GoogleCalendarEventDto;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.CalendarEvent;
import com.hszadkowski.iwa_backend.models.CalendarToken;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.CalendarEventRepository;
import com.hszadkowski.iwa_backend.repos.CalendarTokenRepository;
import com.hszadkowski.iwa_backend.repos.UserRepository;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GoogleCalendarServiceImpl implements GoogleCalendarService {

    private static final String APPLICATION_NAME = "Makeup Appointment Booking";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String PROVIDER_GOOGLE = "google";

    @Value("${google.calendar.client.id}")
    private String clientId;

    @Value("${google.calendar.client.secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect.uri}")
    private String redirectUri;

    private final UserRepository userRepository;
    private final CalendarTokenRepository calendarTokenRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public String getAuthorizationUrl(String userEmail) {
        try {
            GoogleAuthorizationCodeFlow flow = createFlow();
            return flow.newAuthorizationUrl()
                    .setRedirectUri(redirectUri)
                    .setState(userEmail)
                    .build();
        } catch (Exception e) {
            log.error("Error creating authorization URL: ", e);
            throw new RuntimeException("Failed to create authorization URL", e);
        }
    }

    @Override
    public CalendarTokenResponseDto handleOAuthCallback(String authCode, String userEmail)
            throws IOException, GeneralSecurityException {

        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GoogleAuthorizationCodeFlow flow = createFlow();
        GoogleTokenResponse tokenResponse = flow.newTokenRequest(authCode)
                .setRedirectUri(redirectUri)
                .execute();

        // Get user info to store email
        Credential credential = flow.createAndStoreCredential(tokenResponse, userEmail);

        // Get the user's actual Google email using OAuth2 API
        String googleEmail;
        try {
            Oauth2 oauth2 = new Oauth2.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Userinfo userinfo = oauth2.userinfo().get().execute();
            googleEmail = userinfo.getEmail();

            log.info("Retrieved Google email: {} for user: {}", googleEmail, userEmail);
        } catch (Exception e) {
            log.warn("Could not retrieve Google email via OAuth2 API, using provided email", e);
            googleEmail = userEmail; // Fallback to provided email
        }

        // Delete existing token if any
        calendarTokenRepository.findByAppUserAndProvider(user, PROVIDER_GOOGLE)
                .ifPresent(calendarTokenRepository::delete);

        // Save new token
        CalendarToken token = new CalendarToken();
        token.setAppUser(user);
        token.setProvider(PROVIDER_GOOGLE);
        token.setAccessToken(tokenResponse.getAccessToken());
        token.setRefreshToken(tokenResponse.getRefreshToken());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
        token.setEmail(googleEmail); // Store the actual Google email

        CalendarToken savedToken = calendarTokenRepository.save(token);

        return new CalendarTokenResponseDto(
                true,
                PROVIDER_GOOGLE,
                savedToken.getExpiresAt(),
                savedToken.getEmail()
        );
    }

    @Override
    public CalendarTokenResponseDto saveAccessToken(String accessToken, String userEmail)
            throws IOException, GeneralSecurityException {

        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create credential with the access token
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setJsonFactory(JSON_FACTORY)
                .setTransport(httpTransport)
                .build();
        
        credential.setAccessToken(accessToken);

        // Get the user's actual Google email using OAuth2 API
        String googleEmail;
        try {
            Oauth2 oauth2 = new Oauth2.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Userinfo userinfo = oauth2.userinfo().get().execute();
            googleEmail = userinfo.getEmail();

            log.info("Retrieved Google email: {} for user: {}", googleEmail, userEmail);
        } catch (Exception e) {
            log.warn("Could not retrieve Google email via OAuth2 API, using provided email", e);
            googleEmail = userEmail; // Fallback to provided email
        }

        // Delete existing token if any
        calendarTokenRepository.findByAppUserAndProvider(user, PROVIDER_GOOGLE)
                .ifPresent(calendarTokenRepository::delete);

        // Save new token
        CalendarToken token = new CalendarToken();
        token.setAppUser(user);
        token.setProvider(PROVIDER_GOOGLE);
        token.setAccessToken(accessToken);
        // Note: We don't have a refresh token with implicit flow
        token.setRefreshToken(null);
        // Access tokens from implicit flow typically expire in 1 hour
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setEmail(googleEmail);

        CalendarToken savedToken = calendarTokenRepository.save(token);

        return new CalendarTokenResponseDto(
                true,
                PROVIDER_GOOGLE,
                savedToken.getExpiresAt(),
                savedToken.getEmail()
        );
    }

    @Override
    public GoogleCalendarEventDto createCalendarEvent(Appointment appointment, String userEmail)
            throws IOException, GeneralSecurityException {

        Calendar calendarService = getCalendarService(userEmail);
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Event event = new Event()
                .setSummary(appointment.getService().getName() + " Appointment")
                .setDescription(buildEventDescription(appointment));

        if (appointment.getLocation() != null) {
            event.setLocation(appointment.getLocation());
        }

        DateTime startDateTime = new DateTime(
                appointment.getSlot().getStartTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );

        DateTime endDateTime = new DateTime(
                appointment.getSlot().getEndTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );

        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(ZoneId.systemDefault().getId());
        event.setEnd(end);

        EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("email").setMinutes(24 * 60), // 1 day before
                new EventReminder().setMethod("popup").setMinutes(60), // 1 hour before
        };

        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(List.of(reminderOverrides));
        event.setReminders(reminders);


        String calendarId = "primary";
        Event createdEvent = calendarService.events().insert(calendarId, event).execute();

        CalendarEvent calendarEvent = new CalendarEvent();
        calendarEvent.setAppointment(appointment);
        calendarEvent.setAppUser(user);
        calendarEvent.setProvider(PROVIDER_GOOGLE);
        calendarEvent.setExternalEventId(createdEvent.getId());
        calendarEvent.setCalendarId(calendarId);
        calendarEvent.setSynced(true);

        calendarEventRepository.save(calendarEvent);

        return mapToEventDto(createdEvent, calendarId);
    }

    @Override
    public GoogleCalendarEventDto updateCalendarEvent(Appointment appointment, String userEmail)
            throws IOException, GeneralSecurityException {

        Optional<CalendarEvent> calendarEventOpt = calendarEventRepository
                .findByAppointmentAndProvider(appointment, PROVIDER_GOOGLE);

        if (calendarEventOpt.isEmpty()) {
            return createCalendarEvent(appointment, userEmail);
        }

        CalendarEvent calendarEvent = calendarEventOpt.get();
        Calendar calendarService = getCalendarService(userEmail);

        Event existingEvent = calendarService.events()
                .get(calendarEvent.getCalendarId(), calendarEvent.getExternalEventId())
                .execute();

        existingEvent.setSummary(appointment.getService().getName() + " Appointment")
                .setDescription(buildEventDescription(appointment))
                .setLocation(appointment.getLocation());


        DateTime startDateTime = new DateTime(
                appointment.getSlot().getStartTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );

        DateTime endDateTime = new DateTime(
                appointment.getSlot().getEndTime()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );

        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(ZoneId.systemDefault().getId());
        existingEvent.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(ZoneId.systemDefault().getId());
        existingEvent.setEnd(end);

        Event updatedEvent = calendarService.events()
                .update(calendarEvent.getCalendarId(), calendarEvent.getExternalEventId(), existingEvent)
                .execute();

        return mapToEventDto(updatedEvent, calendarEvent.getCalendarId());
    }

    @Override
    public void deleteCalendarEvent(Appointment appointment, String userEmail)
            throws IOException, GeneralSecurityException {

        Optional<CalendarEvent> calendarEventOpt = calendarEventRepository
                .findByAppointmentAndProvider(appointment, PROVIDER_GOOGLE);

        if (calendarEventOpt.isEmpty()) {
            log.warn("No calendar event found for appointment {}", appointment.getAppointmentId());
            return;
        }

        CalendarEvent calendarEvent = calendarEventOpt.get();
        Calendar calendarService = getCalendarService(userEmail);

        try {
            calendarService.events()
                    .delete(calendarEvent.getCalendarId(), calendarEvent.getExternalEventId())
                    .execute();
        } catch (Exception e) {
            log.warn("Failed to delete event from Google Calendar: {}", e.getMessage());
        }

        calendarEventRepository.delete(calendarEvent);
    }

    @Override
    public List<GoogleCalendarEventDto> getUserCalendars(String userEmail)
            throws IOException, GeneralSecurityException {

        Calendar calendarService = getCalendarService(userEmail);

        CalendarList calendarList = calendarService.calendarList().list().execute();

        return calendarList.getItems().stream()
                .map(calendar -> new GoogleCalendarEventDto(
                        calendar.getId(),
                        calendar.getSummary(),
                        calendar.getDescription(),
                        null, // location not applicable for calendar list
                        null, // start time not applicable
                        null, // end time not applicable
                        calendar.getId(),
                        "active"
                ))
                .toList();
    }

    @Override
    public boolean isUserConnectedToGoogleCalendar(String userEmail) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<CalendarToken> tokenOpt = calendarTokenRepository
                .findByAppUserAndProvider(user, PROVIDER_GOOGLE);

        return tokenOpt.isPresent() && tokenOpt.get().getExpiresAt().isAfter(LocalDateTime.now());
    }

    @Override
    public void disconnectGoogleCalendar(String userEmail) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        calendarTokenRepository.deleteByAppUserAndProvider(user, PROVIDER_GOOGLE);

        // Also delete all calendar events for this user
        List<CalendarEvent> events = calendarEventRepository.findByAppUserAndProvider(user, PROVIDER_GOOGLE);
        calendarEventRepository.deleteAll(events);
    }

    @Override
    public void refreshAccessTokenIfNeeded(String userEmail) throws IOException, GeneralSecurityException {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<CalendarToken> tokenOpt = calendarTokenRepository
                .findByAppUserAndProvider(user, PROVIDER_GOOGLE);

        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("No Google Calendar token found for user");
        }

        CalendarToken token = tokenOpt.get();

        // Check if token is expired or expires soon (within 5 minutes)
        if (token.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            // If we don't have a refresh token (implicit flow), we can't refresh
            if (token.getRefreshToken() == null) {
                log.warn("Cannot refresh token for user {} - no refresh token available (implicit flow)", userEmail);
                // Delete the expired token
                calendarTokenRepository.delete(token);
                throw new RuntimeException("Token expired and cannot be refreshed. Please reconnect Google Calendar.");
            }

            GoogleAuthorizationCodeFlow flow = createFlow();

            try {
                GoogleTokenResponse tokenResponse = flow.newTokenRequest(token.getRefreshToken())
                        .setGrantType("refresh_token")
                        .execute();

                // Update token
                token.setAccessToken(tokenResponse.getAccessToken());
                token.setExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));

                if (tokenResponse.getRefreshToken() != null) {
                    token.setRefreshToken(tokenResponse.getRefreshToken());
                }

                calendarTokenRepository.save(token);

            } catch (Exception e) {
                log.error("Failed to refresh Google Calendar token for user {}: {}", userEmail, e.getMessage());
                // Delete invalid token
                calendarTokenRepository.delete(token);
                throw new RuntimeException("Failed to refresh Google Calendar token", e);
            }
        }
    }

    @Override
    public CalendarTokenResponseDto getCalendarConnectionStatus(String userEmail) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<CalendarToken> tokenOpt = calendarTokenRepository
                .findByAppUserAndProvider(user, PROVIDER_GOOGLE);

        if (tokenOpt.isEmpty()) {
            return new CalendarTokenResponseDto(false, null, null, null);
        }

        CalendarToken token = tokenOpt.get();
        boolean isValid = token.getExpiresAt().isAfter(LocalDateTime.now());

        return new CalendarTokenResponseDto(
                isValid,
                token.getProvider(),
                token.getExpiresAt(),
                token.getEmail()
        );
    }

    @Override
    public boolean isAppointmentSynced(Integer appointmentId, String userEmail) {
        try {
            if (!isUserConnectedToGoogleCalendar(userEmail)) {
                return false;
            }

            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElse(null);

            if (appointment == null) {
                return false;
            }


            if (!appointment.getAppUser().getEmail().equals(userEmail)) {
                return false;
            }

            return calendarEventRepository
                    .findByAppointmentAndProvider(appointment, PROVIDER_GOOGLE)
                    .isPresent();

        } catch (Exception e) {
            log.error("Error checking appointment sync status: ", e);
            return false;
        }
    }

    @Override
    @Transactional
    public int syncExistingAppointments(String userEmail) {
        int syncedCount = 0;

        try {
            if (!isUserConnectedToGoogleCalendar(userEmail)) {
                throw new RuntimeException("User is not connected to Google Calendar");
            }

            AppUser user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Appointment> futureAppointments = appointmentRepository
                    .findByAppUserAndScheduledAtAfterAndStatusNameNot(
                            user,
                            LocalDate.now(),
                            "CANCELLED"
                    );

            for (Appointment appointment : futureAppointments) {
                try {
                    boolean alreadySynced = calendarEventRepository
                            .findByAppointmentAndProvider(appointment, PROVIDER_GOOGLE)
                            .isPresent();

                    if (!alreadySynced && appointment.getSlot() != null) {
                        createCalendarEvent(appointment, userEmail);
                        syncedCount++;
                        log.info("Synced appointment {} to Google Calendar", appointment.getAppointmentId());
                    }
                } catch (Exception e) {
                    log.error("Failed to sync appointment {}: {}",
                            appointment.getAppointmentId(), e.getMessage());
                }
            }

            log.info("Successfully synced {} appointments for user {}", syncedCount, userEmail);
            return syncedCount;

        } catch (Exception e) {
            log.error("Error syncing existing appointments for user {}: ", userEmail, e);
            throw new RuntimeException("Failed to sync existing appointments", e);
        }
    }

    // Helper methods
    private GoogleAuthorizationCodeFlow createFlow() throws IOException, GeneralSecurityException {
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                        .setClientId(clientId)
                        .setClientSecret(clientSecret));

        return new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                Arrays.asList(
                        CalendarScopes.CALENDAR,
                        "https://www.googleapis.com/auth/userinfo.email"
                ))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }

    private Calendar getCalendarService(String userEmail) throws IOException, GeneralSecurityException {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CalendarToken token = calendarTokenRepository.findByAppUserAndProvider(user, PROVIDER_GOOGLE)
                .orElseThrow(() -> new RuntimeException("No Google Calendar token found"));

        // Refresh token if needed
        refreshAccessTokenIfNeeded(userEmail);

        // Reload token after potential refresh
        token = calendarTokenRepository.findByAppUserAndProvider(user, PROVIDER_GOOGLE)
                .orElseThrow(() -> new RuntimeException("No Google Calendar token found"));

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleAuthorizationCodeFlow flow = createFlow();

        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setJsonFactory(JSON_FACTORY)
                .setTransport(httpTransport)
                .setClientAuthentication(flow.getClientAuthentication())
                .setTokenServerUrl(new GenericUrl(flow.getTokenServerEncodedUrl()))
                .build();

        credential.setAccessToken(token.getAccessToken());
        credential.setRefreshToken(token.getRefreshToken());

        return new Calendar.Builder(
                httpTransport,
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private String buildEventDescription(Appointment appointment) {
        StringBuilder description = new StringBuilder();
        description.append("Makeup Appointment Details:\n\n");
        description.append("Service: ").append(appointment.getService().getName()).append("\n");
        description.append("Duration: ").append(appointment.getService().getDurationMin()).append(" minutes\n");
        description.append("Price: $").append(appointment.getService().getPrice()).append("\n");

        if (appointment.getDescription() != null && !appointment.getDescription().trim().isEmpty()) {
            description.append("Notes: ").append(appointment.getDescription()).append("\n");
        }

        description.append("\nStatus: ").append(appointment.getStatus().getName());

        return description.toString();
    }

    private GoogleCalendarEventDto mapToEventDto(Event event, String calendarId) {
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (event.getStart() != null && event.getStart().getDateTime() != null) {
            startTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(event.getStart().getDateTime().getValue()),
                    ZoneId.systemDefault()
            );
        }

        if (event.getEnd() != null && event.getEnd().getDateTime() != null) {
            endTime = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(event.getEnd().getDateTime().getValue()),
                    ZoneId.systemDefault()
            );
        }

        return new GoogleCalendarEventDto(
                event.getId(),
                event.getSummary(),
                event.getDescription(),
                event.getLocation(),
                startTime,
                endTime,
                calendarId,
                event.getStatus()
        );
    }
}