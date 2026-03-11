package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.ChatMessageRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ChatMessageResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ConversationResponse;
import com.smartroomfinder.smartroomfinder.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;


    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(
            @RequestParam UUID user1,
            @RequestParam UUID user2
    ) throws Exception {
        return ResponseEntity.ok(chatService.getConversation(user1, user2));
    }


    @GetMapping("/my")
    public ResponseEntity<List<ConversationResponse>> getMyChats(
            @RequestParam UUID userId
    ) {
        return ResponseEntity.ok(chatService.getMyChats(userId));
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessageResponse> sendMessage(@RequestBody ChatMessageRequest request) throws Exception {
        return ResponseEntity.ok(chatService.saveAndSend(request));
    }
}