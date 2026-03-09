package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.IdentityVerificationRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.IdentityVerificationResponse;
import com.smartroomfinder.smartroomfinder.services.IdentityVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/identity-verification")
@RequiredArgsConstructor
public class IdentityVerificationController {

    private final IdentityVerificationService verificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> submit(
            @Valid @RequestBody IdentityVerificationRequest request) {
        try {
            UUID userId = extractUserId();
            IdentityVerificationResponse result =
                    verificationService.submitVerification(request, userId);
            return ResponseEntity.ok(
                    ApiResponse.success(result, "Gửi xác minh thành công. Chờ duyệt."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> getMyStatus() {
        try {
            UUID userId = extractUserId();
            IdentityVerificationResponse result =
                    verificationService.getMyVerification(userId);
            return ResponseEntity.ok(ApiResponse.success(result, "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Hệ thống gặp lỗi", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> approve(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    verificationService.approveVerification(id),
                    "Xác minh đã được duyệt, user được nâng lên LANDLORD"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> reject(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    verificationService.rejectVerification(id, reason),
                    "Yêu cầu đã bị từ chối"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    // Giống hệt AddressController
    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new IllegalStateException("Unauthorized: no authentication found");
        }
        return UUID.fromString(auth.getCredentials().toString());
    }
}