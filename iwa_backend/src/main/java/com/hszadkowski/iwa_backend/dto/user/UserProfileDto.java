package com.hszadkowski.iwa_backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private String name;
    private String surname;
    private String email;
    private String phoneNum;
    private boolean smsNotificationsEnabled;
}
