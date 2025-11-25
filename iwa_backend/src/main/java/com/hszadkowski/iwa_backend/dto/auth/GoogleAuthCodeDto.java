package com.hszadkowski.iwa_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthCodeDto {
    private String authCode;
    private String redirectUri;
}
