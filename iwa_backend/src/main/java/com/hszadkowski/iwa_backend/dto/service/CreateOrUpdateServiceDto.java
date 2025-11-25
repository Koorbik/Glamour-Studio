package com.hszadkowski.iwa_backend.dto.service;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdateServiceDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    private Integer minDuration;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    // TODO: think to add func to add pictures
}
