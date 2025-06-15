package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.AppointmentResponseDto;
import com.hszadkowski.iwa_backend.dto.BookAppointmentDto;
import com.hszadkowski.iwa_backend.dto.RescheduleAppointmentDto;
import com.hszadkowski.iwa_backend.dto.UpdateAppointmentStatusDto;
import com.hszadkowski.iwa_backend.exceptions.AppointmentNotFoundException;
import com.hszadkowski.iwa_backend.models.*;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.AppointmentStatusRepository;
import com.hszadkowski.iwa_backend.repos.AvailabilitySlotRepository;
import com.hszadkowski.iwa_backend.repos.UserRepository;
import com.hszadkowski.iwa_backend.services.interfaces.AppointmentService;
import com.hszadkowski.iwa_backend.services.interfaces.AvailabilityService;
import com.hszadkowski.iwa_backend.services.interfaces.EmailService;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentStatusRepository appointmentStatusRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AvailabilityService availabilityService;
    private final EmailService emailService;
    private final GoogleCalendarService googleCalendarService;

    @Override
    public AppointmentResponseDto bookAppointment(BookAppointmentDto request, String userEmail) {

        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!availabilityService.canBookSlot(request.getSlotId())) {
            throw new RuntimeException("This time slot is no longer available or has already passed");
        }

        AvailabilitySlot slot = availabilitySlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new RuntimeException("Availability slot not found"));

        if (slot.getIsBooked()) {
            throw new RuntimeException("This time slot is no longer available");
        }

        if (!slot.getService().getServiceId().equals(request.getServiceId())) {
            throw new RuntimeException("Service mismatch with selected slot");
        }

        Service service = slot.getService();

        AppointmentStatus status = appointmentStatusRepository.findByName("CONFIRMED")
                .orElseThrow(() -> new RuntimeException("Default appointment status not found"));

        Appointment appointment = new Appointment();
        appointment.setAppUser(user);
        appointment.setService(service);
        appointment.setStatus(status);
        appointment.setLocation(request.getLocation());
        appointment.setScheduledAt(slot.getStartTime().toLocalDate());
        appointment.setDescription(request.getDescription());
        appointment.setSlot(slot);

        slot.setIsBooked(true);
        availabilitySlotRepository.save(slot);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Send confirmation email
        sendBookingConfirmationEmail(savedAppointment);

        // Sync to Google Calendar if user is connected
        syncAppointmentToGoogleCalendar(savedAppointment, userEmail, "create");

        return mapToResponseDto(savedAppointment);
    }

    @Override
    public AppointmentResponseDto rescheduleAppointment(Integer appointmentId, RescheduleAppointmentDto rescheduleDto, String userEmail) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment with ID " + appointmentId + " not found"));

        if (!appointment.getAppUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("You can only reschedule your own appointments");
        }

        if ("CANCELLED".equals(appointment.getStatus().getName()) ||
                "COMPLETED".equals(appointment.getStatus().getName())) {
            throw new RuntimeException("Cannot reschedule a " + appointment.getStatus().getName().toLowerCase() + " appointment");
        }

        if (!availabilityService.canBookSlot(rescheduleDto.getNewSlotId())) {
            throw new RuntimeException("The selected time slot is no longer available or has already passed");
        }

        AvailabilitySlot newSlot = availabilitySlotRepository.findById(rescheduleDto.getNewSlotId())
                .orElseThrow(() -> new RuntimeException("New availability slot not found"));

        if (newSlot.getIsBooked()) {
            throw new RuntimeException("The selected time slot is no longer available");
        }

        if (!newSlot.getService().getServiceId().equals(rescheduleDto.getServiceId()) ||
                !appointment.getService().getServiceId().equals(rescheduleDto.getServiceId())) {
            throw new RuntimeException("Service mismatch");
        }

        AvailabilitySlot oldSlot = appointment.getSlot();
        if (oldSlot != null) {
            oldSlot.setIsBooked(false);
            availabilitySlotRepository.save(oldSlot);
        }

        newSlot.setIsBooked(true);
        availabilitySlotRepository.save(newSlot);

        appointment.setSlot(newSlot);
        appointment.setScheduledAt(newSlot.getStartTime().toLocalDate());

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        sendRescheduleNotificationEmail(updatedAppointment, oldSlot);

        // Update Google Calendar event if user is connected
        syncAppointmentToGoogleCalendar(updatedAppointment, userEmail, "update");

        return mapToResponseDto(updatedAppointment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getUserAppointments(String userEmail) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return appointmentRepository.findByAppUser(user)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointmentById(Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment with ID " + appointmentId + " not found"));
        return mapToResponseDto(appointment);
    }

    @Override
    public void cancelAppointment(Integer appointmentId, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment with ID " + appointmentId + " not found"));

        // Get the user who is making the cancellation request
        AppUser requestingUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user can cancel this appointment (own appointment or admin user)
        boolean isOwner = appointment.getAppUser().getEmail().equals(userEmail);
        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(requestingUser.getRole());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only cancel your own appointments");
        }

        AppointmentStatus cancelledStatus = appointmentStatusRepository.findByName("CANCELLED")
                .orElseThrow(() -> new RuntimeException("Cancelled status not found"));

        appointment.setStatus(cancelledStatus);
        appointmentRepository.save(appointment);

        releaseSlotForAppointment(appointment);

        sendCancellationEmail(appointment);

        // Delete from Google Calendar of the appointment owner
        syncAppointmentToGoogleCalendar(appointment, appointment.getAppUser().getEmail(), "delete");
    }

    @Override
    public AppointmentResponseDto updateAppointmentStatus(Integer appointmentId, UpdateAppointmentStatusDto statusUpdate) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment with ID " + appointmentId + " not found"));

        AppointmentStatus newStatus = appointmentStatusRepository.findByName(statusUpdate.getStatus().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Status '" + statusUpdate.getStatus() + "' not found"));

        if ("CANCELLED".equalsIgnoreCase(statusUpdate.getStatus())) {
            releaseSlotForAppointment(appointment);
            // Delete from Google Calendar for the user
            syncAppointmentToGoogleCalendar(appointment, appointment.getAppUser().getEmail(), "delete");
        }

        appointment.setStatus(newStatus);
        Appointment updatedAppointment = appointmentRepository.save(appointment);

        // Update Google Calendar event if status changed but not cancelled
        if (!"CANCELLED".equalsIgnoreCase(statusUpdate.getStatus())) {
            syncAppointmentToGoogleCalendar(updatedAppointment, appointment.getAppUser().getEmail(), "update");
        }

        return mapToResponseDto(updatedAppointment);
    }

    @Override
    public Map<String, Object> syncAppointmentToCalendar(Integer appointmentId, String userEmail) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check if user is connected to Google Calendar
            if (!googleCalendarService.isUserConnectedToGoogleCalendar(userEmail)) {
                result.put("success", false);
                result.put("error", "Not connected to Google Calendar");
                return result;
            }

            // Get the appointment
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(
                            "Appointment with ID " + appointmentId + " not found"));

            // Verify the appointment belongs to the user
            if (!appointment.getAppUser().getEmail().equals(userEmail)) {
                throw new AccessDeniedException("You can only sync your own appointments");
            }

            // Check if appointment is in a valid state for syncing
            if ("CANCELLED".equals(appointment.getStatus().getName())) {
                result.put("success", false);
                result.put("error", "Cannot sync cancelled appointment");
                return result;
            }

            // Check if already synced
            boolean alreadySynced = googleCalendarService.isAppointmentSynced(appointmentId, userEmail);
            
            // Use the existing helper method for the actual sync
            // For manual sync, we always use "create" action which will create or update
            syncAppointmentToGoogleCalendar(appointment, userEmail, alreadySynced ? "update" : "create");
            
            // Verify the sync was successful by checking if it's now synced
            if (googleCalendarService.isAppointmentSynced(appointmentId, userEmail)) {
                result.put("success", true);
                result.put("calendarEventId", appointmentId.toString()); // We don't have direct access to calendar event ID here
                log.info("Successfully synced appointment {} to Google Calendar for user {}", 
                        appointmentId, userEmail);
            } else {
                result.put("success", false);
                result.put("error", "Sync completed but appointment not found in calendar");
            }
            
        } catch (AccessDeniedException e) {
            log.error("Access denied for appointment {}: {}", appointmentId, e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to sync appointment {} to calendar: {}", appointmentId, e.getMessage());
            result.put("success", false);
            result.put("error", "Failed to sync appointment: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> syncAllAppointmentsToCalendar(String userEmail) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Check if user is connected to Google Calendar
            if (!googleCalendarService.isUserConnectedToGoogleCalendar(userEmail)) {
                result.put("success", false);
                result.put("error", "Not connected to Google Calendar");
                result.put("syncedCount", 0);
                result.put("failedCount", 0);
                return result;
            }

            // Sync existing appointments using the GoogleCalendarService method
            int syncedCount = googleCalendarService.syncExistingAppointments(userEmail);
            
            result.put("success", true);
            result.put("syncedCount", syncedCount);
            result.put("failedCount", 0); // The implementation doesn't track failed syncs
            
            log.info("Successfully synced {} appointments for user {}", syncedCount, userEmail);
            
        } catch (Exception e) {
            log.error("Failed to sync all appointments for user {}: {}", userEmail, e.getMessage());
            result.put("success", false);
            result.put("error", "Failed to sync appointments: " + e.getMessage());
            result.put("syncedCount", 0);
            result.put("failedCount", 0);
        }
        
        return result;
    }

    // Helper methods

    private void releaseSlotForAppointment(Appointment appointment) {
        if (appointment.getSlot() != null) {
            AvailabilitySlot slot = appointment.getSlot();
            slot.setIsBooked(false);
            availabilitySlotRepository.save(slot);
        }
    }

    private void syncAppointmentToGoogleCalendar(Appointment appointment, String userEmail, String action) {
        try {
            if (googleCalendarService.isUserConnectedToGoogleCalendar(userEmail)) {
                switch (action.toLowerCase()) {
                    case "create":
                        googleCalendarService.createCalendarEvent(appointment, userEmail);
                        log.info("Created Google Calendar event for appointment {}", appointment.getAppointmentId());
                        break;
                    case "update":
                        googleCalendarService.updateCalendarEvent(appointment, userEmail);
                        log.info("Updated Google Calendar event for appointment {}", appointment.getAppointmentId());
                        break;
                    case "delete":
                        googleCalendarService.deleteCalendarEvent(appointment, userEmail);
                        log.info("Deleted Google Calendar event for appointment {}", appointment.getAppointmentId());
                        break;
                    default:
                        log.warn("Unknown calendar sync action: {}", action);
                }
            }
        } catch (Exception e) {
            log.error("Failed to sync appointment {} to Google Calendar: {}",
                    appointment.getAppointmentId(), e.getMessage());
            // Don't fail the main operation if calendar sync fails
        }
    }

    private void sendBookingConfirmationEmail(Appointment appointment) {
        try {
            String subject = "Appointment Confirmation - " + appointment.getService().getName();
            String htmlMessage = buildConfirmationEmailHtml(appointment);
            emailService.sendVerificationEmail(appointment.getAppUser().getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            // Log error but don't fail the booking
            log.error("Failed to send booking confirmation email: {}", e.getMessage());
        }
    }

    private void sendRescheduleNotificationEmail(Appointment appointment, AvailabilitySlot oldSlot) {
        try {
            String subject = "Appointment Rescheduled - " + appointment.getService().getName();
            String htmlMessage = buildRescheduleEmailHtml(appointment, oldSlot);
            emailService.sendVerificationEmail(appointment.getAppUser().getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            log.error("Failed to send reschedule notification email: {}", e.getMessage());
        }
    }

    private void sendCancellationEmail(Appointment appointment) {
        try {
            String subject = "Appointment Cancelled - " + appointment.getService().getName();
            String htmlMessage = buildCancellationEmailHtml(appointment);
            emailService.sendVerificationEmail(appointment.getAppUser().getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            log.error("Failed to send cancellation email: {}", e.getMessage());
        }
    }

    // Email template methods || transfer them to email service in the future

    private String buildConfirmationEmailHtml(Appointment appointment) {
        String calendarSync = googleCalendarService.isUserConnectedToGoogleCalendar(appointment.getAppUser().getEmail())
                ? "<p style=\"color: #28a745;\">✓ This appointment has been added to your Google Calendar</p>"
                : "<p style=\"color: #6c757d;\">Connect your Google Calendar in your account settings to automatically sync appointments</p>";

        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Appointment Confirmed!</h2>"
                + "<p style=\"font-size: 16px;\">Your appointment has been successfully booked.</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Appointment Details:</h3>"
                + "<p><strong>Service:</strong> " + appointment.getService().getName() + "</p>"
                + "<p><strong>Date:</strong> " + appointment.getScheduledAt() + "</p>"
                + "<p><strong>Time:</strong> " + appointment.getSlot().getStartTime().toLocalTime() + " - " + appointment.getSlot().getEndTime().toLocalTime() + "</p>"
                + "<p><strong>Location:</strong> " + appointment.getLocation() + "</p>"
                + "<p><strong>Price:</strong> $" + appointment.getService().getPrice() + "</p>"
                + (appointment.getDescription() != null ? "<p><strong>Notes:</strong> " + appointment.getDescription() + "</p>" : "")
                + "</div>"
                + "<div style=\"margin-top: 15px;\">" + calendarSync + "</div>"
                + "<p style=\"font-size: 14px; margin-top: 20px;\">If you need to reschedule or cancel, please contact us or use your account dashboard.</p>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private String buildRescheduleEmailHtml(Appointment appointment, AvailabilitySlot oldSlot) {
        String calendarSync = googleCalendarService.isUserConnectedToGoogleCalendar(appointment.getAppUser().getEmail())
                ? "<p style=\"color: #28a745;\">✓ Your Google Calendar has been updated with the new appointment time</p>"
                : "<p style=\"color: #6c757d;\">Connect your Google Calendar in your account settings to automatically sync appointment changes</p>";

        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Appointment Rescheduled</h2>"
                + "<p style=\"font-size: 16px;\">Your appointment has been successfully rescheduled.</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">New Appointment Details:</h3>"
                + "<p><strong>Service:</strong> " + appointment.getService().getName() + "</p>"
                + "<p><strong>New Date:</strong> " + appointment.getScheduledAt() + "</p>"
                + "<p><strong>New Time:</strong> " + appointment.getSlot().getStartTime().toLocalTime() + " - " + appointment.getSlot().getEndTime().toLocalTime() + "</p>"
                + "<p><strong>Location:</strong> " + appointment.getLocation() + "</p>"
                + "<hr style=\"margin: 15px 0;\">"
                + "<p style=\"color: #666;\"><strong>Previous Time:</strong> " + oldSlot.getStartTime().toLocalDate() + " at " + oldSlot.getStartTime().toLocalTime() + "</p>"
                + "</div>"
                + "<div style=\"margin-top: 15px;\">" + calendarSync + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private String buildCancellationEmailHtml(Appointment appointment) {
        String calendarSync = googleCalendarService.isUserConnectedToGoogleCalendar(appointment.getAppUser().getEmail())
                ? "<p style=\"color: #28a745;\">✓ This appointment has been removed from your Google Calendar</p>"
                : "";

        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Appointment Cancelled</h2>"
                + "<p style=\"font-size: 16px;\">Your appointment has been cancelled as requested.</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Cancelled Appointment:</h3>"
                + "<p><strong>Service:</strong> " + appointment.getService().getName() + "</p>"
                + "<p><strong>Date:</strong> " + appointment.getScheduledAt() + "</p>"
                + "<p><strong>Time:</strong> " + appointment.getSlot().getStartTime().toLocalTime() + "</p>"
                + "</div>"
                + "<div style=\"margin-top: 15px;\">" + calendarSync + "</div>"
                + "<p style=\"font-size: 14px; margin-top: 20px;\">We're sorry to see you go! Feel free to book another appointment anytime.</p>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    private AppointmentResponseDto mapToResponseDto(Appointment appointment) {
        return new AppointmentResponseDto(
                appointment.getAppointmentId(),
                appointment.getAppUser().getAppUserId(),
                appointment.getAppUser().getName() + " " + appointment.getAppUser().getSurname(),
                appointment.getService().getServiceId(),
                appointment.getService().getName(),
                appointment.getService().getDescription(),
                appointment.getService().getDurationMin(),
                appointment.getService().getPrice(),
                appointment.getStatus().getName(),
                appointment.getLocation(),
                appointment.getScheduledAt(),
                appointment.getDescription()
        );
    }
}