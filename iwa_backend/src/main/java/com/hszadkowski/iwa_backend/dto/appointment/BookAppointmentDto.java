package com.hszadkowski.iwa_backend.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookAppointmentDto {

    @NotNull(message = "Slot ID is required")
    private Integer slotId;

    @NotNull(message = "Service ID is required")
    private Integer serviceId;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location cannot exceed 200 characters")
    private String location;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}
