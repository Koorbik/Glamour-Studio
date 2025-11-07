package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.user.UserProfileUpdateDto;
import com.hszadkowski.iwa_backend.models.AppUser;

public interface UserService {
    AppUser findByEmail(String email);
    AppUser updateUserProfile(String email, UserProfileUpdateDto updateDto);
}
