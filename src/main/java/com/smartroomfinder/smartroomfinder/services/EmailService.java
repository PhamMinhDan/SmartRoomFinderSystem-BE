package com.smartroomfinder.smartroomfinder.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendChatNotification(String to, String senderName) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("📩 Bạn có tin nhắn mới - Smart Room Finder");
        msg.setText("Bạn có một tin nhắn tìm trọ từ " + senderName +
                ".\nTruy cập Smart Room Finder để xem ngay!");
        mailSender.send(msg);
    }
}
