package com.hszadkowski.iwa_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSignUpResponseDto {
    private Integer id;
    private String name;
    private String surname;
    private String email;
    private String phoneNum;
    private String role;
    private String verificationCode; // TODO: just for testing comfort, delete later
}