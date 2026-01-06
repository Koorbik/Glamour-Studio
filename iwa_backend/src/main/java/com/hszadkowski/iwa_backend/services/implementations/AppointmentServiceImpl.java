package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.appointment.AppointmentResponseDto;
import com.hszadkowski.iwa_backend.dto.appointment.BookAppointmentDto;
import com.hszadkowski.iwa_backend.dto.appointment.RescheduleAppointmentDto;
import com.hszadkowski.iwa_backend.dto.appointment.UpdateAppointmentStatusDto;
import com.hszadkowski.iwa_backend.exceptions.AppointmentNotFoundException;
import com.hszadkowski.iwa_backend.models.*;
import com.hszadkowski.iwa_backend.repos.*;
import com.hszadkowski.iwa_backend.services.interfaces.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final EmailTemplateService emailTemplateService;
    private final GoogleCalendarService googleCalendarService;
    private final PayUService payUService;
    private final ContractService contractService;
    private final PaymentRepository paymentRepository;

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

        if (!Boolean.TRUE.equals(request.getAcceptsTerms())) {
            throw new RuntimeException("You must accept the terms and conditions.");
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);

        byte[] contractPdf = null;
        try {
            contractPdf = contractService.generateContract(savedAppointment);
        } catch (Exception e) {
            log.error("Failed to generate contract", e);
        }

        // Send confirmation email
        sendBookingConfirmationEmail(savedAppointment, contractPdf);

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

        Payment payment = appointment.getPayment();
        if (payment != null && "COMPLETED".equals(payment.getStatus())) {
            handleRefund(appointment, payment);
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

    private void handleRefund(Appointment appointment, Payment payment) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime appointmentTime = appointment.getSlot().getStartTime();

            long hoursDifference = ChronoUnit.HOURS.between(now, appointmentTime);

            BigDecimal refundAmount;
            String description;
            String paymentStatusUpdate;

            if (hoursDifference >= 24) {
                // Full refund
                refundAmount = payment.getAmount();
                description = "Full refund for cancellation (>24h before appointment)";
                paymentStatusUpdate = "REFUNDED";
            } else {
                // 50% refund
                refundAmount = payment.getAmount().multiply(new BigDecimal("0.5"));
                description = "50% refund for late cancellation (<24h before appointment)";
                paymentStatusUpdate = "PARTIALLY_REFUNDED";
            }

            payUService.refundTransaction(payment.getTransactionId(), refundAmount, description);

            payment.setStatus(paymentStatusUpdate);
            paymentRepository.save(payment);

            log.info("Processed refund for appointment {}: {} ({})",
                    appointment.getAppointmentId(), refundAmount, description);

            sendRefundEmail(appointment, refundAmount, description);

        } catch (Exception e) {
            log.error("Failed to process refund for appointment {}: {}",
                    appointment.getAppointmentId(), e.getMessage());
        }
    }

    private void sendRefundEmail(Appointment appointment, BigDecimal amount, String reason) {
        try {
            String subject = "Refund Processed - " + appointment.getService().getName();
            String htmlMessage = emailTemplateService.buildRefundEmail(appointment, amount, reason);
            emailService.sendVerificationEmail(appointment.getAppUser().getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            log.error("Failed to send refund email: {}", e.getMessage());
        }
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

    private void sendBookingConfirmationEmail(Appointment appointment, byte[] contractPdf) {
        try {
            String subject = "Appointment Confirmation - " + appointment.getService().getName();
            String htmlMessage = emailTemplateService.buildBookingConfirmationEmail(appointment);

            if (contractPdf != null) {
                emailService.sendEmailWithAttachment(
                        appointment.getAppUser().getEmail(),
                        subject,
                        htmlMessage,
                        "Service_Agreement.pdf",
                        contractPdf
                );
            } else {
                emailService.sendVerificationEmail(appointment.getAppUser().getEmail(), subject, htmlMessage);
            }
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email: {}", e.getMessage());
        }
    }

    private void sendRescheduleNotificationEmail(Appointment appointment, AvailabilitySlot oldSlot) {
        try {
            String subject = "Appointment Rescheduled - " + appointment.getService().getName();
            String htmlMessage = emailTemplateService.buildRescheduleEmail(appointment, oldSlot);
            emailService.sendVerificationEmail(appointment.getAppUser().getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            log.error("Failed to send reschedule notification email: {}", e.getMessage());
        }
    }

    private void sendCancellationEmail(Appointment appointment) {
        try {
            String subject = "Appointment Cancelled - " + appointment.getService().getName();
            String htmlMessage = emailTemplateService.buildCancellationEmail(appointment);
            emailService.sendVerificationEmail(appointment.getAppUser().getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            log.error("Failed to send cancellation email: {}", e.getMessage());
        }
    }

    private AppointmentResponseDto mapToResponseDto(Appointment appointment) {
        String paymentStatus = "UNPAID";
        String paymentMethod = null;
        Integer paymentId = null;

        if (appointment.getPayment() != null) {
            paymentStatus = appointment.getPayment().getStatus();
            paymentId = appointment.getPayment().getPaymentId();

            if (appointment.getPayment().getPaymentMethod() != null) {
                paymentMethod = appointment.getPayment().getPaymentMethod().name();
            }
        }

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
                appointment.getDescription(),
                paymentStatus,
                paymentMethod,
                paymentId
        );
    }
}