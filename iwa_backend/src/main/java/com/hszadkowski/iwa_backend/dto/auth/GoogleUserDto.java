package com.hszadkowski.iwa_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserDto {
    private String id;
    private String email;
    private String name;
    private String givenName;
    private String familyName;
    private String accessToken;
    private String phoneNum;
}