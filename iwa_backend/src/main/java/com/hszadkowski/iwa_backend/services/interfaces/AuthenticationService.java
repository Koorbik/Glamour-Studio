package com.hszadkowski.iwa_backend.services.interfaces;

import com.hszadkowski.iwa_backend.dto.auth.*;
import com.hszadkowski.iwa_backend.models.AppUser;

public interface AuthenticationService {

    UserSignUpResponseDto signUpFacebookUser(FacebookUserDto facebookUser);

    UserSignUpResponseDto signUp(RegisterUserRequestDto request);

    UserSignUpResponseDto signUpGoogleUser(GoogleUserDto googleUser);

    AppUser authenticate(LoginUserDto request);

    void verifyUser(VerifyUserDto request);

    void resendVerificationCode(String email);

    void sendPasswordResetEmail(String email);

    void resetPassword(String email, String code, String newPassword);

}
