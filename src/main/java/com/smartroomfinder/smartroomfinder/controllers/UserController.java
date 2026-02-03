package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.UserResponse;
import com.smartroomfinder.smartroomfinder.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(
                                "Missing or invalid authorization header",
                                "UNAUTHORIZED"
                        ));
            }

            String token = authHeader.substring(7);
            log.info("Get current user request");

            UserResponse userResponse = userService.getCurrentUser(token);

            return ResponseEntity.ok(
                    ApiResponse.success(userResponse, "User information retrieved successfully")
            );

        } catch (RuntimeException e) {
            log.warn("Error retrieving user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            "Failed to retrieve user information",
                            e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            "An unexpected error occurred",
                            e.getMessage()
                    ));
        }
    }
}