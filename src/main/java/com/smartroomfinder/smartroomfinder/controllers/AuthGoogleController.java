package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.RefreshTokenRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.AuthGoogleResponse;
import com.smartroomfinder.smartroomfinder.exceptions.UserBannedException;
import com.smartroomfinder.smartroomfinder.services.AuthGoogleService;
import com.smartroomfinder.smartroomfinder.services.UserService;
import com.smartroomfinder.smartroomfinder.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthGoogleController {

    private final AuthGoogleService authGoogleService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<?>> googleLogin(
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

        } catch (Exception e) {
            Throwable cause = unwrap(e);

            if (cause instanceof UserBannedException bannedEx) {
                log.warn("Banned user attempted login");

                Map<String, Object> banInfo = new HashMap<>();
                banInfo.put("ban_reason", bannedEx.getBanReason() != null && !bannedEx.getBanReason().isBlank()
                        ? bannedEx.getBanReason()
                        : "Vi phạm quy định sử dụng dịch vụ");
                banInfo.put("banned_at", bannedEx.getBannedAt() != null
                        ? bannedEx.getBannedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : null);

                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Tài khoản của bạn đã bị khóa", "USER_BANNED", banInfo));
            }

            if (cause instanceof GeneralSecurityException || cause instanceof IOException) {
                log.error("Google login error: {}", cause.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Failed to authenticate with Google", cause.getMessage()));
            }

            log.error("Unexpected error during Google login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred", e.getMessage()));
        }
    }

    private Throwable unwrap(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
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

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body("Refresh token is missing");
            }

            Claims claims = jwtUtil.extractAllClaims(refreshToken);

            String type = claims.get("type", String.class);
            if (!"REFRESH".equals(type)) {
                return ResponseEntity.status(401).body("Invalid token type");
            }

            String username = claims.get("username", String.class);
            String userId = claims.get("userId", String.class);
            Integer tokenVersion = claims.get("tokenVersion", Integer.class);

            String newAccessToken = jwtUtil.generateAccessToken(username, userId, tokenVersion);

            Map<String, String> response = new HashMap<>();
            response.put("accessToken", newAccessToken);

            return ResponseEntity.ok(response);

        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(401).body("Refresh token expired");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
    }
}