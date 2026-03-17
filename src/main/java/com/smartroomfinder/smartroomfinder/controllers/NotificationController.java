package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.response.NotificationResponse;
import com.smartroomfinder.smartroomfinder.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public List<NotificationResponse> getMy(@RequestParam UUID userId) {
        return service.getMy(userId);
    }

    @GetMapping("/unread-count")
    public long count(@RequestParam UUID userId) {
        return service.countUnread(userId);
    }

    @PatchMapping("/read-all")
    public void readAll(@RequestParam UUID userId) {
        service.markAllAsRead(userId);
    }
}
