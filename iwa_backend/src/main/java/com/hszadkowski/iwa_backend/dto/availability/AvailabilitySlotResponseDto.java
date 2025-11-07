package com.hszadkowski.iwa_backend.dto.availability;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AvailabilitySlotResponseDto {
    private Integer slotId;
    private Integer serviceId;
    private String serviceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    @JsonProperty("isBooked")
    private boolean isBooked;
}
