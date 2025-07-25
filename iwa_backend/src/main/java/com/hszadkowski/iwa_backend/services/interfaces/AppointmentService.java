package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.AppointmentResponseDto;
import com.hszadkowski.iwa_backend.dto.BookAppointmentDto;
import com.hszadkowski.iwa_backend.dto.RescheduleAppointmentDto;
import com.hszadkowski.iwa_backend.dto.UpdateAppointmentStatusDto;

import java.util.List;
import java.util.Map;

public interface AppointmentService {

    AppointmentResponseDto bookAppointment(BookAppointmentDto request, String userEmail);

    List<AppointmentResponseDto> getUserAppointments(String userEmail);

    List<AppointmentResponseDto> getAllAppointments();

    AppointmentResponseDto getAppointmentById(Integer appointmentId);

    void cancelAppointment(Integer appointmentId, String userEmail);

    AppointmentResponseDto rescheduleAppointment(Integer appointmentId, RescheduleAppointmentDto rescheduleDto, String userEmail);

    AppointmentResponseDto updateAppointmentStatus(Integer appointmentId, UpdateAppointmentStatusDto statusUpdate);

    Map<String, Object> syncAppointmentToCalendar(Integer appointmentId, String userEmail);

    Map<String, Object> syncAllAppointmentsToCalendar(String userEmail);
}
