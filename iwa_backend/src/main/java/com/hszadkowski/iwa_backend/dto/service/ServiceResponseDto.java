package com.hszadkowski.iwa_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponseDto {
    private Integer serviceId;
    private String name;
    private String description;
    private Integer minDuration;
    private BigDecimal price;
}
