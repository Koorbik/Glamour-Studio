package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AvailabilitySlot;
import com.hszadkowski.iwa_backend.services.interfaces.EmailTemplateService;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private final GoogleCalendarService googleCalendarService;


    @Override
    public String buildBookingConfirmationEmail(Appointment appointment) {
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

    @Override
    public String buildRescheduleEmail(Appointment appointment, AvailabilitySlot oldSlot) {
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

    @Override
    public String buildCancellationEmail(Appointment appointment) {
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

    @Override
    public String buildRefundEmail(Appointment appointment, BigDecimal amount, String reason) {
        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Refund Processed</h2>"
                + "<p style=\"font-size: 16px;\">A refund has been initiated for your cancelled appointment.</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Refund Details:</h3>"
                + "<p><strong>Service:</strong> " + appointment.getService().getName() + "</p>"
                + "<p><strong>Original Date:</strong> " + appointment.getScheduledAt() + "</p>"
                + "<p><strong>Refund Amount:</strong> $" + amount + "</p>"
                + "<p><strong>Reason:</strong> " + reason + "</p>"
                + "</div>"
                + "<p style=\"font-size: 14px; margin-top: 20px;\">Please allow 3-5 business days for the funds to appear in your account.</p>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    @Override
    public String buildReminderEmailHtml(Appointment appointment) {
        LocalDateTime appointmentDateTime = appointment.getSlot().getStartTime();

        return "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">🔔 Appointment Reminder</h2>"
                + "<p style=\"font-size: 16px;\">Hi " + appointment.getAppUser().getName() + ",</p>"
                + "<p style=\"font-size: 16px;\">This is a friendly reminder that you have an appointment scheduled for <strong>tomorrow</strong>!</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); border-left: 4px solid #007bff;\">"
                + "<h3 style=\"color: #333; margin-top: 0;\">Appointment Details:</h3>"
                + "<p><strong>Service:</strong> " + appointment.getService().getName() + "</p>"
                + "<p><strong>Date:</strong> " + appointment.getScheduledAt() + "</p>"
                + "<p><strong>Time:</strong> " + appointmentDateTime.toLocalTime() + "</p>"
                + "<p><strong>Duration:</strong> " + appointment.getService().getDurationMin() + " minutes</p>"
                + "<p><strong>Location:</strong> " + appointment.getLocation() + "</p>"
                + (appointment.getDescription() != null && !appointment.getDescription().trim().isEmpty()
                ? "<p><strong>Notes:</strong> " + appointment.getDescription() + "</p>" : "")
                + "</div>"
                + "<div style=\"background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin-top: 20px;\">"
                + "<h4 style=\"color: #1976d2; margin-top: 0;\">💡 Preparation Tips:</h4>"
                + "<ul style=\"color: #333; padding-left: 20px;\">"
                + "<li>Please arrive 5-10 minutes early</li>"
                + "<li>Come with a clean face (if applicable)</li>"
                + "<li>Bring any specific makeup preferences or inspiration photos</li>"
                + "</ul>"
                + "</div>"
                + "<p style=\"font-size: 14px; margin-top: 20px; color: #666;\">"
                + "Need to reschedule or cancel? Please contact us as soon as possible or use your account dashboard."
                + "</p>"
                + "<p style=\"font-size: 14px; color: #666;\">"
                + "We look forward to seeing you tomorrow!"
                + "</p>"
                + "</div>"
                + "</body>"
                + "</html>";
    }
}
