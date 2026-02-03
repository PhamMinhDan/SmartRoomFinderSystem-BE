package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.FacebookLoginRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.AuthFacebookResponse;
import com.smartroomfinder.smartroomfinder.services.AuthFacebookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/facebook")
@RequiredArgsConstructor
public class AuthFacebookController {

    private final AuthFacebookService authFacebookService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthFacebookResponse>> facebookLogin(
            @Valid @RequestBody FacebookLoginRequest request) {

        log.info("Facebook login request for email: {}", request.getEmail());

        AuthFacebookResponse response = authFacebookService.processOAuthLogin(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "Đăng nhập Facebook thành công")
        );
    }
}