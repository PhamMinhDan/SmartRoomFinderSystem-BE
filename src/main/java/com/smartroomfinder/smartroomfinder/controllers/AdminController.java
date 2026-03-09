package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.response.AdminRoomResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.IdentityVerificationResponse;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import com.smartroomfinder.smartroomfinder.services.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    // ── GET /api/admin/stats ──────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminService.AdminStats>> getStats() {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(adminService.getStats(), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        }
    }

    // ── GET /api/admin/rooms?isApproved=false ─────────────────────
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<Page<AdminRoomResponse>>> getRooms(
            @RequestParam(required = false) Boolean isApproved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    adminService.getAllRooms(isApproved, page, size), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        }
    }

    // ── GET /api/admin/rooms/{id} ─────────────────────────────────
    @GetMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<AdminRoomResponse>> getRoomDetail(@PathVariable Long id) {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(adminService.getRoomDetail(id), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        }
    }

    // ── PATCH /api/admin/rooms/{id}/approve ───────────────────────
    @PatchMapping("/rooms/{id}/approve")
    public ResponseEntity<ApiResponse<AdminRoomResponse>> approveRoom(@PathVariable Long id) {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    adminService.approveRoom(id), "Phòng đã được duyệt"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    // ── PATCH /api/admin/rooms/{id}/reject ────────────────────────
    @PatchMapping("/rooms/{id}/reject")
    public ResponseEntity<ApiResponse<AdminRoomResponse>> rejectRoom(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String reason) {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    adminService.rejectRoom(id, reason), "Phòng đã bị từ chối"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    // ── PATCH /api/admin/verifications/{id}/approve ───────────────
    @PatchMapping("/verifications/{id}/approve")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> approveVerification(
            @PathVariable Long id) {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    adminService.approveVerification(id),
                    "Xác minh duyệt, user được nâng lên LANDLORD"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    // ── PATCH /api/admin/verifications/{id}/reject ────────────────
    @PatchMapping("/verifications/{id}/reject")
    public ResponseEntity<ApiResponse<IdentityVerificationResponse>> rejectVerification(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    adminService.rejectVerification(id, reason), "Yêu cầu đã bị từ chối"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    // ── Check ADMIN role từ SecurityContext ───────────────────────
    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new IllegalStateException("Unauthorized");
        }
        UUID userId = UUID.fromString(auth.getCredentials().toString());
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String roleName = user.getRole_id() != null ? user.getRole_id().getRoleName() : "";
        if (!"ADMIN".equals(roleName)) {
            throw new IllegalStateException("Forbidden: ADMIN role required");
        }
    }
}