package com.hszadkowski.iwa_backend.dto.calendar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarTokenDto {
    private String accessToken;
}
