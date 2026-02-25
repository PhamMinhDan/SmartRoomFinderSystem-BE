package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.AuthGoogleResponse;
import com.smartroomfinder.smartroomfinder.dto.response.UserResponse;
import com.smartroomfinder.smartroomfinder.services.AuthGoogleService;
import com.smartroomfinder.smartroomfinder.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthGoogleController {

    private final AuthGoogleService authGoogleService;
    private final UserService userService;

    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<AuthGoogleResponse>> googleLogin(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "Missing or invalid authorization header",
                                "INVALID_AUTH_HEADER"
                        ));
            }

            String idToken = authHeader.substring(7);
            log.info("Google login request received");

            AuthGoogleResponse authGGResponse = authGoogleService.loginWithGoogle(idToken);

            return ResponseEntity.ok(
                    ApiResponse.success(authGGResponse, "Google login successful")
            );

        } catch (GeneralSecurityException | IOException e) {
            log.error("Google login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            "Failed to authenticate with Google",
                            e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Unexpected error during Google login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "An unexpected error occurred",
                            e.getMessage()
                    ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "Missing or invalid authorization header",
                            "INVALID_AUTH_HEADER"
                    ));
        }

        String token = authHeader.substring(7);
        log.info("Logout request received");

        authGoogleService.logout(token);

        return ResponseEntity.ok(
                ApiResponse.success("Logout successful", "User logged out successfully")
        );
    }

}