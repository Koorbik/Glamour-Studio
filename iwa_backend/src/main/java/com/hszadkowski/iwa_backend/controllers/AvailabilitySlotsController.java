package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.availability.AvailabilitySlotResponseDto;
import com.hszadkowski.iwa_backend.dto.availability.CreateAvailabilitySlotDto;
import com.hszadkowski.iwa_backend.dto.availability.GetAvailableSlotsDto;
import com.hszadkowski.iwa_backend.services.interfaces.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilitySlotsController {

    private final AvailabilityService availabilityService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AvailabilitySlotResponseDto> createAvailabilitySlot(@RequestBody @Valid CreateAvailabilitySlotDto createAvailabilitySlotDto,
                                                                              Authentication authentication) {
        String adminEmail = authentication.getName();

        AvailabilitySlotResponseDto availabilitySlot = availabilityService.createAvailabilitySlot(createAvailabilitySlotDto, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(availabilitySlot);
    }

    @GetMapping
    public ResponseEntity<List<AvailabilitySlotResponseDto>> getAvailableSlots(
            @ModelAttribute @Valid GetAvailableSlotsDto getAvailableSlotsDto) {
        List<AvailabilitySlotResponseDto> slots = availabilityService.getAvailableSlots(getAvailableSlotsDto);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/all")
    public ResponseEntity<List<AvailabilitySlotResponseDto>> getAllSlots() {
        List<AvailabilitySlotResponseDto> slots = availabilityService.getAllSlots();
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<AvailabilitySlotResponseDto>> getSlotsByService(
            @PathVariable Integer serviceId) {
        List<AvailabilitySlotResponseDto> slots = availabilityService.getSlotsByService(serviceId);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/{slotId}/check")
    public ResponseEntity<Boolean> checkSlotAvailability(@PathVariable Integer slotId) {
        boolean isAvailable = availabilityService.isSlotAvailable(slotId);
        return ResponseEntity.ok(isAvailable);
    }

    @PutMapping("/{slotId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AvailabilitySlotResponseDto> updateSlot(
            @PathVariable Integer slotId,
            @RequestBody @Valid CreateAvailabilitySlotDto updateDto) {
        AvailabilitySlotResponseDto updatedSlot = availabilityService.updateSlot(slotId, updateDto);
        return ResponseEntity.ok(updatedSlot);
    }

    @PutMapping("/{slotId}/book")
    @PreAuthorize("hasRole('ADMIN')") // should it be protected like this???
    public ResponseEntity<Void> markSlotAsBooked(@PathVariable Integer slotId) {
        availabilityService.markSlotAsBooked(slotId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{slotId}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markSlotAsAvailable(@PathVariable Integer slotId) {
        availabilityService.markSlotAsAvailable(slotId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{slotId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSlot(@PathVariable Integer slotId) {
        availabilityService.deleteSlot(slotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{slotId}/can-book")
    public ResponseEntity<Boolean> canBookSlot(@PathVariable Integer slotId) {
        boolean canBook = availabilityService.canBookSlot(slotId);
        return ResponseEntity.ok(canBook);
    }
}
