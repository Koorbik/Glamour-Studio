package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AppointmentStatus;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.AppointmentStatusRepository;
import com.hszadkowski.iwa_backend.services.interfaces.AppointmentCleanupService;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentCleanupServiceImpl implements AppointmentCleanupService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusRepository appointmentStatusRepository;
    private final GoogleCalendarService googleCalendarService;

    @Override
    @Scheduled(fixedRate = 900000) // 15 mins
    @Transactional
    public void cancelUnpaidAppointments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<Appointment> appointments = appointmentRepository.findUnpaidConfirmedAppointmentsOlderThan(threshold);

        if (!appointments.isEmpty()) {
            log.info("Found {} unpaid appointments to cleanup", appointments.size());
        }

        for (Appointment appt : appointments) {
            try {
                AppointmentStatus cancelledStatus = appointmentStatusRepository.findByName("CANCELLED")
                        .orElseThrow(() -> new RuntimeException("CANCELLED status not found"));
                appt.setStatus(cancelledStatus);

                if (appt.getSlot() != null) {
                    appt.getSlot().setIsBooked(false);
                }

                appointmentRepository.save(appt);

                try {
                    String userEmail = appt.getAppUser().getEmail();
                    if (googleCalendarService.isUserConnectedToGoogleCalendar(userEmail)) {
                        googleCalendarService.deleteCalendarEvent(appt, userEmail);
                        log.info("Removed calendar event for unpaid appointment {}", appt.getAppointmentId());
                    }
                } catch (Exception e) {
                    log.error("Failed to remove calendar event for appointment {}: {}",
                            appt.getAppointmentId(), e.getMessage());
                }

                log.info("Successfully cancelled unpaid appointment ID: {}", appt.getAppointmentId());

            } catch (Exception e) {
                log.error("Error cleaning up appointment {}: {}", appt.getAppointmentId(), e.getMessage());
            }
        }
    }
}