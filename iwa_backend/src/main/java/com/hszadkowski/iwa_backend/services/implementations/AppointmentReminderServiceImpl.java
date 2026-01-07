package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AppointmentStatus;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.AppointmentStatusRepository;
import com.hszadkowski.iwa_backend.services.interfaces.AppointmentReminderService;
import com.hszadkowski.iwa_backend.services.interfaces.EmailService;
import com.hszadkowski.iwa_backend.services.interfaces.EmailTemplateService;
import com.hszadkowski.iwa_backend.services.interfaces.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderServiceImpl implements AppointmentReminderService {

    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final AppointmentStatusRepository appointmentStatusRepository;
    private final SmsService smsService;

    /**
     * Runs every day at 10:00 AM to send reminders for appointments scheduled for tomorrow
     */
    @Override
    @Scheduled(cron = "0 0 10 * * *") // 10:00 AM daily
    public void sendDailyReminders() {
        log.info("Starting daily appointment reminder process...");

        LocalDate tomorrow = LocalDate.now().plusDays(1);

        AppointmentStatus confirmedStatus = appointmentStatusRepository.findByName("CONFIRMED")
                .orElse(null);

        if (confirmedStatus == null) {
            log.warn("CONFIRMED status not found in database");
            return;
        }

        // Find all confirmed appointments for tomorrow using repository method
        List<Appointment> tomorrowAppointments = appointmentRepository
                .findByScheduledAtAndStatus(tomorrow, confirmedStatus);

        log.info("Found {} confirmed appointments for tomorrow ({})",
                tomorrowAppointments.size(), tomorrow);

        for (Appointment appointment : tomorrowAppointments) {
            try {
                sendReminder(appointment);
                log.info("Reminder sent for appointment ID: {}", appointment.getAppointmentId());
            } catch (Exception e) {
                log.error("Failed to send reminder for appointment ID: {} - Error: {}",
                        appointment.getAppointmentId(), e.getMessage());
            }
        }

        log.info("Daily reminder process completed");
    }

    private void sendReminder(Appointment appointment) {
        try {
            String subject = "Reminder: Your appointment is tomorrow - " + appointment.getService().getName();
            String htmlMessage = emailTemplateService.buildReminderEmailHtml(appointment);
            emailService.sendVerificationEmail(appointment.getAppUser().getEmail(), subject, htmlMessage);

            String phoneNumber = appointment.getAppUser().getPhoneNum();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                String smsMessage = String.format("Hi %s, reminder for your appointment tomorrow at %s. Service: %s. See you soon! - Glamour Studio",
                        appointment.getAppUser().getName(),
                        appointment.getSlot().getStartTime().toLocalTime(),
                        appointment.getService().getName()
                );

                smsService.sendSms(phoneNumber, smsMessage);
            }

        } catch (Exception e) {
            log.error("Failed to send notification for appointment ID: {} - Error: {}",
                    appointment.getAppointmentId(), e.getMessage());
        }
    }


}
