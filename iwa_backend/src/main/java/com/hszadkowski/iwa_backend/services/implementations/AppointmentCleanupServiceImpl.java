package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AppointmentStatus;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.AppointmentStatusRepository;
import com.hszadkowski.iwa_backend.services.interfaces.AppointmentCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentCleanupServiceImpl implements AppointmentCleanupService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusRepository appointmentStatusRepository;

    @Override
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void cancelUnpaidAppointments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
        List<Appointment> appointments = appointmentRepository.findUnpaidConfirmedAppointmentsOlderThan(threshold);

        for (Appointment appt : appointments) {
            AppointmentStatus cancelledStatus = appointmentStatusRepository.findByName("CANCELLED")
                    .orElseThrow(() -> new RuntimeException("CANCELLED status not found"));
            appt.setStatus(cancelledStatus);
            if (appt.getSlot() != null) appt.getSlot().setIsBooked(false);
            appointmentRepository.save(appt);
        }
    }
}