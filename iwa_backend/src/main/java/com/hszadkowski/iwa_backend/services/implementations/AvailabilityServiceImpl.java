package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.availability.AvailabilitySlotResponseDto;
import com.hszadkowski.iwa_backend.dto.availability.CreateAvailabilitySlotDto;
import com.hszadkowski.iwa_backend.dto.availability.GetAvailableSlotsDto;
import com.hszadkowski.iwa_backend.exceptions.ServiceDoesNotExistException;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.models.Appointment;
import com.hszadkowski.iwa_backend.models.AppointmentStatus;
import com.hszadkowski.iwa_backend.models.AvailabilitySlot;
import com.hszadkowski.iwa_backend.models.Service;
import com.hszadkowski.iwa_backend.repos.AppointmentRepository;
import com.hszadkowski.iwa_backend.repos.AppointmentStatusRepository;
import com.hszadkowski.iwa_backend.repos.AvailabilitySlotRepository;
import com.hszadkowski.iwa_backend.repos.ServiceRepository;
import com.hszadkowski.iwa_backend.repos.UserRepository;
import com.hszadkowski.iwa_backend.services.interfaces.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
public class AvailabilityServiceImpl implements AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatusRepository appointmentStatusRepository;

    @Override
    public AvailabilitySlotResponseDto createAvailabilitySlot(CreateAvailabilitySlotDto dto, String adminEmail) {
        AppUser admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found")); // change to custom exception in the future

        Service service = serviceRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new ServiceDoesNotExistException("Service not found"));


        validateSlotTimes(dto.getStartTime(), dto.getEndTime());

        checkForOverlappingSlots(admin, dto.getStartTime(), dto.getEndTime());

        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setAppUser(admin);
        slot.setService(service);
        slot.setStartTime(dto.getStartTime());
        slot.setEndTime(dto.getEndTime());
        slot.setIsBooked(false);

        AvailabilitySlot savedSlot = availabilitySlotRepository.save(slot);
        return mapToResponseDto(savedSlot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponseDto> getAvailableSlots(GetAvailableSlotsDto dto) {
        List<AvailabilitySlot> slots;

        if (dto.getServiceId() != null) {
            Service service = serviceRepository.findById(dto.getServiceId())
                    .orElseThrow(() -> new ServiceDoesNotExistException("Service not found"));
            slots = availabilitySlotRepository.findByServiceAndIsBookedFalseAndStartTimeBetween(
                    service, dto.getStartTime(), dto.getEndTime());
        } else {
            slots = availabilitySlotRepository.findByIsBookedFalseAndStartTimeBetween(
                    dto.getStartTime(), dto.getEndTime());
        }

        return slots.stream()
                .filter(slot -> !slot.getIsBooked() && slot.getStartTime().isAfter(LocalDateTime.now()))
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponseDto> getAllSlots() {
        return availabilitySlotRepository.findAll()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteSlot(Integer slotId) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.getIsBooked()) {
            throw new RuntimeException("Cannot delete a booked slot");
        }

        availabilitySlotRepository.delete(slot);
    }

    @Override
    public AvailabilitySlotResponseDto updateSlot(Integer slotId, CreateAvailabilitySlotDto dto) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.getIsBooked()) {
            throw new RuntimeException("Cannot update a booked slot");
        }

        validateSlotTimes(dto.getStartTime(), dto.getEndTime());

        checkForOverlappingSlotsExcludingCurrent(slot.getAppUser(), dto.getStartTime(), dto.getEndTime(), slotId);

        if (!slot.getService().getServiceId().equals(dto.getServiceId())) {
            Service newService = serviceRepository.findById(dto.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            slot.setService(newService);
        }

        slot.setStartTime(dto.getStartTime());
        slot.setEndTime(dto.getEndTime());

        AvailabilitySlot updatedSlot = availabilitySlotRepository.save(slot);
        return mapToResponseDto(updatedSlot);
    }

    @Override
    public void markSlotAsBooked(Integer slotId) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.getIsBooked()) {
            throw new RuntimeException("Slot is already booked");
        }

        slot.setIsBooked(true);
        availabilitySlotRepository.save(slot);
    }

    @Override
    public void markSlotAsAvailable(Integer slotId) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        // Check if there's an active appointment for this slot
        Optional<Appointment> activeAppointment = appointmentRepository.findBySlotAndStatusNameNotIn(
                slot, Arrays.asList("CANCELLED", "COMPLETED"));
        
        if (activeAppointment.isPresent()) {
            // Cancel the appointment if it exists
            Appointment appointment = activeAppointment.get();
            AppointmentStatus cancelledStatus = appointmentStatusRepository.findByName("CANCELLED")
                    .orElseThrow(() -> new RuntimeException("Cancelled status not found"));
            appointment.setStatus(cancelledStatus);
            appointmentRepository.save(appointment);
        }

        slot.setIsBooked(false);
        availabilitySlotRepository.save(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(Integer slotId) {
        return availabilitySlotRepository.findById(slotId)
                .map(slot -> !slot.getIsBooked())
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponseDto> getSlotsByService(Integer serviceId) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        return availabilitySlotRepository.findByService(service)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean canBookSlot(Integer slotId) {
        return availabilitySlotRepository.findById(slotId)
                .map(slot -> !slot.getIsBooked() && slot.getStartTime().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    // Helper methods
    private void validateSlotTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot create slots in the past");
        }
    }

    private void checkForOverlappingSlots(AppUser admin, LocalDateTime startTime, LocalDateTime endTime) {
        List<AvailabilitySlot> overlappingSlots = availabilitySlotRepository
                .findOverlappingSlots(admin, startTime, endTime);

        if (!overlappingSlots.isEmpty()) {
            throw new RuntimeException("Time slot overlaps with existing availability");
        }
    }

    private void checkForOverlappingSlotsExcludingCurrent(AppUser admin, LocalDateTime startTime,
                                                          LocalDateTime endTime, Integer excludeSlotId) {
        List<AvailabilitySlot> overlappingSlots = availabilitySlotRepository
                .findOverlappingSlotsExcluding(admin, startTime, endTime, excludeSlotId);

        if (!overlappingSlots.isEmpty()) {
            throw new RuntimeException("Time slot overlaps with existing availability");
        }
    }

    private AvailabilitySlotResponseDto mapToResponseDto(AvailabilitySlot slot) {
        AvailabilitySlotResponseDto dto = new AvailabilitySlotResponseDto(
                slot.getSlotId(),
                slot.getService().getServiceId(),
                slot.getService().getName(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getIsBooked()
        );
        return dto;
    }
}
