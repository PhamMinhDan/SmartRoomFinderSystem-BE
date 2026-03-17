package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.response.NotificationResponse;
import com.smartroomfinder.smartroomfinder.entities.Notification;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.repositories.NotificationRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public void createNotification(UUID userId, String title, String content, String url) {
        Users user = userRepo.findById(userId)
                .orElseThrow();

        Notification noti = repo.save(Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .redirectUrl(url)
                .type("CHAT")
                .build());

        // push realtime
        messagingTemplate.convertAndSend(
                "/topic/notification." + userId,
                map(noti)
        );
    }

    public List<NotificationResponse> getMy(UUID userId) {
        Users user = userRepo.findById(userId).orElseThrow();
        return repo.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::map).toList();
    }

    public long countUnread(UUID userId) {
        Users user = userRepo.findById(userId).orElseThrow();
        return repo.countByUserAndIsReadFalse(user);
    }

    private NotificationResponse map(Notification n) {
        return NotificationResponse.builder()
                .id(n.getNotificationId())
                .title(n.getTitle())
                .content(n.getContent())
                .redirectUrl(n.getRedirectUrl())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        repo.markAllAsRead(userId);
    }
}
