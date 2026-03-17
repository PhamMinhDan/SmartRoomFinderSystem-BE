package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.CreateReportRequest;
import com.smartroomfinder.smartroomfinder.dto.request.ResolveReportRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ReportResponse;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import com.smartroomfinder.smartroomfinder.services.ReportService;
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
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    @PostMapping("/api/rooms/{roomId}/reports")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateReportRequest req) {

        try {
            UUID reporterId = extractUserId();
            ReportResponse response = reportService.createReport(roomId, reporterId, req);
            return ResponseEntity.ok(ApiResponse.success(response, "Báo cáo đã được gửi thành công"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (RuntimeException e) {
            log.error("Error creating report: roomId={}", roomId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        }
    }

    @GetMapping("/api/admin/reports")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size) {

        try {
            // ✅ FIX: Sử dụng requireAdmin() thay vì @PreAuthorize
            requireAdmin();
            
            Page<ReportResponse> reports = reportService.getReports(status, page, size);
            return ResponseEntity.ok(ApiResponse.success(reports, "Lấy danh sách báo cáo thành công"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (Exception e) {
            log.error("Error getting reports", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách báo cáo", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/api/admin/rooms/{roomId}/reports")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> getReportsByRoom(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // ✅ FIX: Sử dụng requireAdmin()
            requireAdmin();
            
            Page<ReportResponse> reports = reportService.getReportsByRoom(roomId, page, size);
            return ResponseEntity.ok(ApiResponse.success(reports, "Lấy báo cáo theo phòng thành công"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (Exception e) {
            log.error("Error getting reports for room: {}", roomId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy báo cáo", "INTERNAL_ERROR"));
        }
    }

    @PatchMapping("/api/admin/reports/{reportId}/resolve")
    public ResponseEntity<ApiResponse<ReportResponse>> resolveReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ResolveReportRequest req) {

        try {
            // ✅ FIX: Sử dụng requireAdmin()
            requireAdmin();
            
            UUID adminId = extractUserId();
            ReportResponse response = reportService.resolveReport(reportId, adminId, req);
            return ResponseEntity.ok(ApiResponse.success(response, "Xử lý báo cáo thành công"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            log.error("Error resolving report: reportId={}", reportId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        }
    }

    // ✅ Helper: Check ADMIN role từ SecurityContext
    private void requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Unauthorized: không tìm thấy authentication");
        }

        Object credentials = auth.getCredentials();
        if (credentials == null) {
            throw new IllegalStateException("Unauthorized: không tìm thấy credentials");
        }

        UUID userId;
        try {
            userId = UUID.fromString(credentials.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unauthorized: định dạng user id không hợp lệ");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String roleName = user.getRole_id() != null ? user.getRole_id().getRoleName() : "";
        if (!"ADMIN".equals(roleName)) {
            log.warn("⚠️  Access denied - User {} has role: {}, required ADMIN", 
                    user.getUsername(), roleName);
            throw new IllegalStateException("Forbidden: ADMIN role required");
        }

        log.debug("✅ Admin check passed for user: {}", user.getUsername());
    }

    // ✅ Helper: Extract userId từ SecurityContext
    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Unauthorized: không tìm thấy authentication");
        }

        Object credentials = auth.getCredentials();
        if (credentials == null) {
            throw new IllegalStateException("Unauthorized: không tìm thấy credentials");
        }

        try {
            return UUID.fromString(credentials.toString());
        } catch (IllegalArgumentException e) {
            log.error("Invalid user id format: {}", credentials);
            throw new IllegalStateException("Unauthorized: định dạng user id không hợp lệ");
        }
    }
}