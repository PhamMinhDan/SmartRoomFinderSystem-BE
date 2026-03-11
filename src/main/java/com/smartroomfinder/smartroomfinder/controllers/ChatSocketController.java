package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.ChatMessageRequest;
import com.smartroomfinder.smartroomfinder.dto.request.MarkReadRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ChatMessageResponse;
import com.smartroomfinder.smartroomfinder.services.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;


    @MessageMapping("/chat.send")
    public void sendMessage(@Payload @Valid ChatMessageRequest request) {
        try {
            chatService.saveAndSend(request);
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
        }
    }


    @MessageMapping("/chat.read")
    public void markRead(@Payload MarkReadRequest request) {
        chatService.markAsRead(request);
    }
}