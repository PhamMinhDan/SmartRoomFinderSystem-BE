package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.response.NotificationResponse;
import com.smartroomfinder.smartroomfinder.entities.Notification;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.repositories.NotificationRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

    // ── Gửi thông báo cho user thường (CHAT, SYSTEM...) ──────────
    public void createNotification(UUID userId, String title, String content, String url) {
        Users user = userRepo.findById(userId).orElseThrow();

        Notification noti = repo.save(Notification.builder()
                .user(user)
                .title(title)
                .content(content)
                .redirectUrl(url)
                .type("SYSTEM")
                .build());

        // Push realtime WebSocket đến user
        messagingTemplate.convertAndSend(
                "/topic/notification." + userId,
                map(noti)
        );
    }

    // ── Gửi thông báo đến TẤT CẢ admin trong hệ thống ───────────
    public void notifyAllAdmins(String title, String content, String url, String type) {
        List<Users> admins = userRepo.findAllAdmins(); // custom query findByRole_id_RoleName("ADMIN")

        for (Users admin : admins) {
            Notification noti = repo.save(Notification.builder()
                    .user(admin)
                    .title(title)
                    .content(content)
                    .redirectUrl(url)
                    .type(type)
                    .build());

            // Push realtime đến admin
            messagingTemplate.convertAndSend(
                    "/topic/notification." + admin.getUserId(),
                    map(noti)
            );

            // Cập nhật unread count realtime cho admin header
            long unread = repo.countByUserAndIsReadFalse(admin);
            messagingTemplate.convertAndSend(
                    "/topic/unread-count." + admin.getUserId(),
                    unread
            );
        }
    }

    // ── Gửi email bất đồng bộ ────────────────────────────────────
    @Async
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ── Gửi email cho tất cả admin ───────────────────────────────
    @Async
    public void sendEmailToAllAdmins(String subject, String text) {
        List<Users> admins = userRepo.findAllAdmins();
        for (Users admin : admins) {
            sendEmail(admin.getEmail(), subject, text);
        }
    }

    // ── Lấy danh sách thông báo của user ─────────────────────────
    public List<NotificationResponse> getMy(UUID userId) {
        Users user = userRepo.findById(userId).orElseThrow();
        return repo.findByUserOrderByCreatedAtDesc(user)
                .stream().map(this::map).toList();
    }

    // ── Đếm chưa đọc ─────────────────────────────────────────────
    public long countUnread(UUID userId) {
        Users user = userRepo.findById(userId).orElseThrow();
        return repo.countByUserAndIsReadFalse(user);
    }

    // ── Đánh dấu tất cả đã đọc ───────────────────────────────────
    @Transactional
    public void markAllAsRead(UUID userId) {
        repo.markAllAsRead(userId);
    }

    // ── Đánh dấu 1 thông báo đã đọc ─────────────────────────────
    @Transactional
    public void markOneAsRead(Long notificationId, UUID userId) {
        repo.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getUserId().equals(userId)) {
                n.setIsRead(true);
                repo.save(n);
            }
        });
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
}