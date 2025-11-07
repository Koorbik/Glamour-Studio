package com.hszadkowski.iwa_backend.services.implementations;

import com.hszadkowski.iwa_backend.dto.auth.*;
import com.hszadkowski.iwa_backend.exceptions.UserAlreadyExistsException;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.repos.UserRepository;
import com.hszadkowski.iwa_backend.services.interfaces.AuthenticationService;
import com.hszadkowski.iwa_backend.services.interfaces.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    public UserSignUpResponseDto signUp(RegisterUserRequestDto input) {

        if (userRepository.existsByEmail(input.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + input.getEmail() + "' is already registered");
        }

        String role = (input.getRole() != null && !input.getRole().isBlank()) ? input.getRole().trim() : "ROLE_USER";

        AppUser user = AppUser.builder()
                .name(input.getName())
                .surname(input.getSurname())
                .email(input.getEmail())
                .phoneNum(input.getPhoneNum())
                .passwordHash(passwordEncoder.encode(input.getPassword()))
                .role(role)
                .build();

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        sendVerificationEmail(user);

        AppUser saved = userRepository.save(user);
        return new UserSignUpResponseDto(saved.getAppUserId(), saved.getName(), saved.getSurname(),
                saved.getEmail(), saved.getPhoneNum(), saved.getRole(), saved.getVerificationCode());
    }

    @Override
    public UserSignUpResponseDto signUpFacebookUser(FacebookUserDto facebookUser) {
        if (userRepository.existsByEmail(facebookUser.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + facebookUser.getEmail() + "' is already registered");
        }

        String[] nameParts = facebookUser.getName().split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        AppUser user = AppUser.builder()
                .name(firstName)
                .surname(lastName)
                .email(facebookUser.getEmail())
                .phoneNum(facebookUser.getPhoneNum() != null ? facebookUser.getPhoneNum() : "")
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Generate random password
                .role("ROLE_USER")
                .build();

        // Facebook users are automatically verified since they're authenticated through
        // Facebook
        user.setEnabled(true);

        // Set password reset code
        String resetCode = generateVerificationCode();
        user.setPasswordResetCode(resetCode);
        user.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        sendPasswordResetEmailToUser(user);

        AppUser saved = userRepository.save(user);
        return new UserSignUpResponseDto(saved.getAppUserId(), saved.getName(), saved.getSurname(),
                saved.getEmail(), saved.getPhoneNum(), saved.getRole(), resetCode);
    }

    @Override
    public UserSignUpResponseDto signUpGoogleUser(GoogleUserDto googleUser) {
        if (userRepository.existsByEmail(googleUser.getEmail())) {
            throw new UserAlreadyExistsException("Email '" + googleUser.getEmail() + "' is already registered");
        }

        AppUser user = AppUser.builder()
                .name(googleUser.getGivenName() != null ? googleUser.getGivenName() : googleUser.getName())
                .surname(googleUser.getFamilyName() != null ? googleUser.getFamilyName() : "")
                .email(googleUser.getEmail())
                .phoneNum(googleUser.getPhoneNum() != null ? googleUser.getPhoneNum() : "") // Use provided phone number or empty string
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Assign a random, unusable password
                .role("ROLE_USER")
                .build();

        // Google users are automatically verified
        user.setEnabled(true);

        // We will not send a password reset email immediately.
        // The user can choose to create a password from their account settings later.
        // For now, they can only log in using SSO

        AppUser saved = userRepository.save(user);

        // Return null for the verification code as it's not needed.
        return new UserSignUpResponseDto(saved.getAppUserId(), saved.getName(), saved.getSurname(),
                saved.getEmail(), saved.getPhoneNum(), saved.getRole(), null);
    }

    @Override
    public AppUser authenticate(LoginUserDto input) {
        AppUser user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please verify your account");

        }
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword()));

        return user;
    }

    @Override
    public void verifyUser(VerifyUserDto input) {
        Optional<AppUser> optionalAppUser = userRepository.findByEmail(input.getEmail());
        if (optionalAppUser.isPresent()) {
            AppUser appUser = optionalAppUser.get();
            if (appUser.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired");
            }
            if (appUser.getVerificationCode().equals(input.getVerificationCode())) {
                appUser.setEnabled(true);
                appUser.setVerificationCode(null);
                appUser.setVerificationCodeExpiresAt(null);
                userRepository.save(appUser);
            } else {
                throw new RuntimeException("Invalid verification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    @Override
    public void resendVerificationCode(String email) {
        Optional<AppUser> optionalAppUser = userRepository.findByEmail(email);
        if (optionalAppUser.isPresent()) {
            AppUser appUser = optionalAppUser.get();
            if (appUser.isEnabled()) {
                throw new RuntimeException("Account is already verified");
            }
            appUser.setVerificationCode(generateVerificationCode());
            appUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
            sendVerificationEmail(appUser);
            userRepository.save(appUser);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    @Override
    public void sendPasswordResetEmail(String email) {
        Optional<AppUser> optionalAppUser = userRepository.findByEmail(email);
        if (optionalAppUser.isPresent()) {
            AppUser appUser = optionalAppUser.get();
            String resetCode = generateVerificationCode();
            appUser.setPasswordResetCode(resetCode);
            appUser.setPasswordResetCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
            sendPasswordResetEmailToUser(appUser);
            userRepository.save(appUser);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        Optional<AppUser> optionalAppUser = userRepository.findByEmail(email);
        if (optionalAppUser.isPresent()) {
            AppUser appUser = optionalAppUser.get();
            if (appUser.getPasswordResetCodeExpiresAt() == null ||
                    appUser.getPasswordResetCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Reset code has expired");
            }
            if (appUser.getPasswordResetCode() != null &&
                    appUser.getPasswordResetCode().equals(code)) {
                appUser.setPasswordHash(passwordEncoder.encode(newPassword));
                appUser.setPasswordResetCode(null);
                appUser.setPasswordResetCodeExpiresAt(null);
                appUser.setEnabled(true);
                userRepository.save(appUser);
            } else {
                throw new RuntimeException("Invalid reset code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    private void sendVerificationEmail(AppUser user) {
        String subject = "Account Verification";
        String verificationCode = "VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }

    private void sendPasswordResetEmailToUser(AppUser user) {
        String subject = "Password Reset Request";
        String resetCode = user.getPasswordResetCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Password Reset</h2>"
                + "<p style=\"font-size: 16px;\">You have requested to reset your password. Please use the code below to set a new password for your account:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Reset Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + resetCode + "</p>"
                + "</div>"
                + "<p style=\"font-size: 14px; margin-top: 20px;\">This code will expire in 15 minutes.</p>"
                + "<p style=\"font-size: 14px;\">If you did not request this password reset, please ignore this email or contact support if you have concerns.</p>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }

    private String generateVerificationCode() { // think of a better way to generate the code, and clearer function name
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}