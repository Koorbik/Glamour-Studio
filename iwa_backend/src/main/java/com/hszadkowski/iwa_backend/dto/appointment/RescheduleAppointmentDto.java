package com.hszadkowski.iwa_backend.dto.appointment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleAppointmentDto {

    @NotNull(message = "New slot ID is required")
    private Integer newSlotId;

    @NotNull(message = "Service ID is required")
    private Integer serviceId;
}