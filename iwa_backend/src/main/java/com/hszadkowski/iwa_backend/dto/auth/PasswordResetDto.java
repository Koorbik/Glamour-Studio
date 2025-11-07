package com.hszadkowski.iwa_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetDto {
    private String email;
    private String code;
    private String newPassword;
}