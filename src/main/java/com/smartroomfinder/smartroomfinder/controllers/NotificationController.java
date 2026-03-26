package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.response.NotificationResponse;
import com.smartroomfinder.smartroomfinder.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    // ── Lấy thông báo của user (truyền userId hoặc lấy từ token) ─
    @GetMapping
    public List<NotificationResponse> getMy(@RequestParam UUID userId) {
        return service.getMy(userId);
    }

    // ── Đếm chưa đọc ─────────────────────────────────────────────
    @GetMapping("/unread-count")
    public long count(@RequestParam UUID userId) {
        return service.countUnread(userId);
    }

    // ── Đánh dấu tất cả đã đọc ───────────────────────────────────
    @PatchMapping("/read-all")
    public void readAll(@RequestParam UUID userId) {
        service.markAllAsRead(userId);
    }

    // ── Đánh dấu 1 đã đọc ────────────────────────────────────────
    @PatchMapping("/{id}/read")
    public void readOne(@PathVariable Long id, @RequestParam UUID userId) {
        service.markOneAsRead(id, userId);
    }

    // ── Admin: lấy unread count của chính admin đang đăng nhập ───
    @GetMapping("/me/unread-count")
    public long myUnreadCount() {
        UUID userId = extractUserId();
        return service.countUnread(userId);
    }

    // ── Admin: lấy danh sách thông báo của chính admin ───────────
    @GetMapping("/me")
    public List<NotificationResponse> myNotifications() {
        UUID userId = extractUserId();
        return service.getMy(userId);
    }

    // ── Admin: đánh dấu tất cả đã đọc (dùng token, không cần userId) ─
    @PatchMapping("/me/read-all")
    public void myReadAll() {
        UUID userId = extractUserId();
        service.markAllAsRead(userId);
    }

    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new IllegalStateException("Unauthorized");
        }
        return UUID.fromString(auth.getCredentials().toString());
    }
}