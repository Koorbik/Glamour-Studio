package com.hszadkowski.iwa_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {

    private String token;
    private long expiresIn;
}
