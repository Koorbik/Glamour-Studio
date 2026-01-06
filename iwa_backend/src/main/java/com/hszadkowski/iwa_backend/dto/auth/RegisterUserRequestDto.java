package com.hszadkowski.iwa_backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequestDto {
    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank @Size(max = 50)
    private String surname;

    @NotBlank @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Must be a valid phone number")
    private String phoneNum;

    @NotBlank @Size(min = 8, max = 100)
    private String password;

    private String role; // TODO: passing role should be done differently to up the security

    private boolean smsNotificationsEnabled;
}