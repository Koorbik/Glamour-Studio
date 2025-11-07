package com.hszadkowski.iwa_backend.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppointmentStatusDto {
    
    @NotBlank(message = "Status is required")
    private String status;
}
