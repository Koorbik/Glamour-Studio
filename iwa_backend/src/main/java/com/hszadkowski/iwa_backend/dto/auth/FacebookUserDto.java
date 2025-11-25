package com.hszadkowski.iwa_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacebookUserDto {
    private String id;
    private String name;
    private String email;
    private String phoneNum;
    private String accessToken;
}