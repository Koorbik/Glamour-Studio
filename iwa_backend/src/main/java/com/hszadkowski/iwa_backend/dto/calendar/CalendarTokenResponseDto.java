package com.hszadkowski.iwa_backend.dto.calendar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarTokenResponseDto {
    private boolean connected;
    private String provider;
    private LocalDateTime expiresAt;
    private String email;
}
