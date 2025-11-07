package com.hszadkowski.iwa_backend.dto.appointment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {
    
    private Integer appointmentId;
    private Integer userId;
    private String userName;
    private Integer serviceId;
    private String serviceName;
    private String serviceDescription;
    private Integer serviceDurationMin;
    private BigDecimal servicePrice;
    private String status;
    private String location;
    private LocalDate scheduledAt;
    private String description;
}
