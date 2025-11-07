package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.availability.AvailabilitySlotResponseDto;
import com.hszadkowski.iwa_backend.dto.availability.CreateAvailabilitySlotDto;
import com.hszadkowski.iwa_backend.dto.availability.GetAvailableSlotsDto;

import java.util.List;

public interface AvailabilityService {

    AvailabilitySlotResponseDto createAvailabilitySlot(CreateAvailabilitySlotDto dto, String adminEmail);

    List<AvailabilitySlotResponseDto> getAvailableSlots(GetAvailableSlotsDto dto);

    List<AvailabilitySlotResponseDto> getAllSlots();

    void deleteSlot(Integer slotId);

    AvailabilitySlotResponseDto updateSlot(Integer slotId, CreateAvailabilitySlotDto dto);

    void markSlotAsBooked(Integer slotId);

    void markSlotAsAvailable(Integer slotId);

    boolean isSlotAvailable(Integer slotId);

    List<AvailabilitySlotResponseDto> getSlotsByService(Integer serviceId);

    boolean canBookSlot(Integer slotId);
}
