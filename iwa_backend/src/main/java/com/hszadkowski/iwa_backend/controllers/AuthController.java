package com.hszadkowski.iwa_backend.controllers;

import com.hszadkowski.iwa_backend.dto.auth.*;
import com.hszadkowski.iwa_backend.models.AppUser;
import com.hszadkowski.iwa_backend.repos.UserRepository;
import com.hszadkowski.iwa_backend.services.interfaces.AuthenticationService;
import com.hszadkowski.iwa_backend.services.interfaces.FacebookService;
import com.hszadkowski.iwa_backend.services.interfaces.GoogleAuthService;
import com.hszadkowski.iwa_backend.services.interfaces.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    private final FacebookService facebookService;
    private final GoogleAuthService googleAuthService;
    public final UserRepository userRepository;

    //in the future deactivate the previous jwt token if user generates another - is it needed?

    @PostMapping("/signup")
    public ResponseEntity<UserSignUpResponseDto> register(@RequestBody RegisterUserRequestDto registerUserDto) {
        UserSignUpResponseDto registeredUser = authenticationService.signUp(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/signup/facebook")
    public ResponseEntity<UserSignUpResponseDto> registerWithFacebook(@RequestBody FacebookUserDto facebookUser) {
        if (!facebookService.validateFacebookToken(facebookUser.getAccessToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FacebookUserDto facebookValidatedUser = facebookService.getFacebookUserInfo(facebookUser.getAccessToken());
        if (facebookValidatedUser == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        UserSignUpResponseDto registeredFacebookUser = authenticationService.signUpFacebookUser(facebookValidatedUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredFacebookUser);

    }

    @PostMapping("/signup/google")
    public ResponseEntity<UserSignUpResponseDto> registerWithGoogle(@RequestBody GoogleUserDto googleUser) {
        if (!googleAuthService.validateGoogleToken(googleUser.getAccessToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GoogleUserDto validatedGoogleUser = googleAuthService.getGoogleUserInfo(googleUser.getAccessToken());
        if (validatedGoogleUser == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        UserSignUpResponseDto registeredGoogleUser = authenticationService.signUpGoogleUser(validatedGoogleUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredGoogleUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> authenticate(@RequestBody LoginUserDto loginUserDto) {
        AppUser authenticatedUser = authenticationService.authenticate(loginUserDto);

        UserDetails userDetails = User.builder()
                .username(authenticatedUser.getEmail())
                .password(authenticatedUser.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(authenticatedUser.getRole())))
                .build();
        String jwtToken = jwtService.generateToken(userDetails);

        LoginResponseDto loginResponse = new LoginResponseDto(jwtToken, jwtService.getExpirationTime());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/oauth/authenticate")
    public ResponseEntity<LoginResponseDto> authenticateWithOAuth(@RequestBody OAuthAuthenticationDto oauthDto) { // TODO: in the future transfer the business logic in this endpoint to a service
        String email = null;
        boolean isValid = false;

        switch (oauthDto.getProvider().toLowerCase()) {
            case "facebook":
                if (!facebookService.validateFacebookToken(oauthDto.getAccessToken())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                FacebookUserDto fbUser = facebookService.getFacebookUserInfo(oauthDto.getAccessToken());
                if (fbUser != null) {
                    email = fbUser.getEmail();
                    isValid = true;
                }
                break;

            case "google":
                if (!googleAuthService.validateGoogleToken(oauthDto.getAccessToken())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                GoogleUserDto googleUser = googleAuthService.getGoogleUserInfo(oauthDto.getAccessToken());
                if (googleUser != null) {
                    email = googleUser.getEmail();
                    isValid = true;
                }
                break;

            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new LoginResponseDto("Invalid provider", 0));
        }

        if (!isValid || email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<AppUser> existingUser = userRepository.findByEmail(email);

        if (existingUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new LoginResponseDto("User not found. Please sign up first.", 0));
        }

        AppUser user = existingUser.get();

        UserDetails userDetails = User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole())))
                .build();

        String jwtToken = jwtService.generateToken(userDetails);
        LoginResponseDto loginResponse = new LoginResponseDto(jwtToken, jwtService.getExpirationTime());

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        try {
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok(Map.of("message", "Account verified successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok(Map.of("message", "Verification code sent"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/pwdresetmail")
    public ResponseEntity<?> sendPasswordResetEmail(@RequestParam String email) {
        try {
            authenticationService.sendPasswordResetEmail(email);
            return ResponseEntity.ok("Password reset email has been sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/pwdreset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetDto passwordResetDto) {
        try {
            authenticationService.resetPassword(
                    passwordResetDto.getEmail(),
                    passwordResetDto.getCode(),
                    passwordResetDto.getNewPassword());
            return ResponseEntity.ok("Password has been set successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/debug")
    public ResponseEntity<?> debugAuth(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            System.out.println("Extracted token: " + token.substring(0, Math.min(50, token.length())) + "...");

            try {
                String username = jwtService.extractUsername(token);
                System.out.println("Extracted username: " + username);
                return ResponseEntity.ok("Token valid for user: " + username);
            } catch (Exception e) {
                System.out.println("Token extraction error: " + e.getMessage());
                return ResponseEntity.badRequest().body("Token error: " + e.getMessage());
            }
        }

        return ResponseEntity.badRequest().body("Authorization header: " + authHeader);
    }
}