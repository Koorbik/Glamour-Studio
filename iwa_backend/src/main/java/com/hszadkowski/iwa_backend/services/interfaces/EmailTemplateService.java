package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AvailabilitySlot;
import java.math.BigDecimal;

public interface EmailTemplateService {
    String buildBookingConfirmationEmail(Appointment appointment);
    String buildRescheduleEmail(Appointment appointment, AvailabilitySlot oldSlot);
    String buildCancellationEmail(Appointment appointment);
    String buildRefundEmail(Appointment appointment, BigDecimal amount, String reason);
    String buildReminderEmailHtml(Appointment appointment);
}