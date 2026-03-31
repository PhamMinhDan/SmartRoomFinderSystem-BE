package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.CreateRoomRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomVersionResponse;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import com.smartroomfinder.smartroomfinder.services.RoomVersionService;
import jakarta.validation.Valid;
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
@RequiredArgsConstructor
public class RoomVersionController {

    private final RoomVersionService VersionService;
    private final UserRepository         userRepository;

    // ══════════════════════════════════════════════════════════════
    // LANDLORD endpoints  →  /api/rooms/{id}/edit-request
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/api/rooms/{id}/edit-request")
    public ResponseEntity<ApiResponse<RoomVersionResponse>> submitVersion(
            @PathVariable Long id,
            @Valid @RequestBody CreateRoomRequest request) {
        try {
            UUID userId = extractUserId();
            RoomVersionResponse resp = VersionService.submitVersion(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success(resp,
                    "Yêu cầu chỉnh sửa đã được gửi. Vui lòng chờ admin phê duyệt."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage(), "CONFLICT"));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    /**
     * Landlord kiểm tra xem phòng có đang có pending edit request không.
     */
    @GetMapping("/api/rooms/{id}/edit-request/pending")
    public ResponseEntity<ApiResponse<RoomVersionResponse>> getPendingEdit(@PathVariable Long id) {
        return VersionService.getPendingEditForRoom(id)
                .map(r -> ResponseEntity.ok(ApiResponse.success(r, "OK")))
                .orElse(ResponseEntity.ok(ApiResponse.success(null, "NO_PENDING")));
    }

    /** Admin lấy danh sách edit requests */
    @GetMapping("/api/admin/edit-requests")
    public ResponseEntity<ApiResponse<Page<RoomVersionResponse>>> getVersions(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size) {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    VersionService.getVersions(status, page, size), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        }
    }

    /** Admin xem chi tiết 1 edit request */
    @GetMapping("/api/admin/edit-requests/{VersionId}")
    public ResponseEntity<ApiResponse<RoomVersionResponse>> getVersionDetail(
            @PathVariable Long VersionId) {
        try {
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    VersionService.getVersionDetail(VersionId), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        }
    }

    /** Admin DUYỆT edit request */
    @PatchMapping("/api/admin/edit-requests/{VersionId}/approve")
    public ResponseEntity<ApiResponse<RoomVersionResponse>> approveVersion(
            @PathVariable Long VersionId) {
        try {
            UUID adminId = extractUserId();
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    VersionService.approveVersion(VersionId, adminId),
                    "Đã phê duyệt yêu cầu chỉnh sửa"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    /** Admin TỪ CHỐI edit request */
    @PatchMapping("/api/admin/edit-requests/{VersionId}/reject")
    public ResponseEntity<ApiResponse<RoomVersionResponse>> rejectVersion(
            @PathVariable Long VersionId,
            @RequestParam(defaultValue = "") String reason) {
        try {
            UUID adminId = extractUserId();
            requireAdmin();
            return ResponseEntity.ok(ApiResponse.success(
                    VersionService.rejectVersion(VersionId, reason, adminId),
                    "Đã từ chối yêu cầu chỉnh sửa"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null)
            throw new IllegalStateException("Unauthorized");
        return UUID.fromString(auth.getCredentials().toString());
    }

    private void requireAdmin() {
        UUID userId = extractUserId();
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        String roleName = user.getRole_id() != null ? user.getRole_id().getRoleName() : "";
        if (!"ADMIN".equals(roleName))
            throw new IllegalStateException("Forbidden: ADMIN role required");
    }
}