package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.calendar.CalendarTokenResponseDto;
import com.hszadkowski.iwa_backend.dto.calendar.GoogleCalendarEventDto;
import com.hszadkowski.iwa_backend.models.Appointment;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface GoogleCalendarService {

    String getAuthorizationUrl(String userEmail);

    CalendarTokenResponseDto handleOAuthCallback(String authCode, String userEmail) throws IOException, GeneralSecurityException;

    CalendarTokenResponseDto saveAccessToken(String accessToken, String userEmail) throws IOException, GeneralSecurityException;

    GoogleCalendarEventDto createCalendarEvent(Appointment appointment, String userEmail) throws IOException, GeneralSecurityException;

    GoogleCalendarEventDto updateCalendarEvent(Appointment appointment, String userEmail) throws IOException, GeneralSecurityException;

    void deleteCalendarEvent(Appointment appointment, String userEmail) throws IOException, GeneralSecurityException;

    List<GoogleCalendarEventDto> getUserCalendars(String userEmail) throws IOException, GeneralSecurityException;

    boolean isUserConnectedToGoogleCalendar(String userEmail);

    void disconnectGoogleCalendar(String userEmail);

    void refreshAccessTokenIfNeeded(String userEmail) throws IOException, GeneralSecurityException;

    CalendarTokenResponseDto getCalendarConnectionStatus(String userEmail);

    boolean isAppointmentSynced(Integer appointmentId, String userEmail);

    int syncExistingAppointments(String userEmail);
}