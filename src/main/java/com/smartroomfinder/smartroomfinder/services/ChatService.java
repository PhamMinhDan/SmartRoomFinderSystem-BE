package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.request.ChatMessageRequest;
import com.smartroomfinder.smartroomfinder.dto.request.MarkReadRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ChatMessageResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ConversationResponse;
import com.smartroomfinder.smartroomfinder.entities.ChatMessage;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.mappers.ChatMapper;
import com.smartroomfinder.smartroomfinder.repositories.ChatRepository;
import com.smartroomfinder.smartroomfinder.repositories.RoomRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepo;
    private final UserRepository userRepo;
    private final RoomRepository roomRepo;
    private final EncryptionService encryptionService;
    private final ChatMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional
    public ChatMessageResponse saveAndSend(ChatMessageRequest req) throws Exception {

        Users sender = userRepo.findById(req.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + req.getSenderId()));

        Users receiver = userRepo.findById(req.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + req.getReceiverId()));

        Rooms room = roomRepo.findById(req.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found: " + req.getRoomId()));

        // Mã hóa trước khi lưu
        String encrypted = encryptionService.encrypt(req.getMessage());

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .room(room)
                .messageContent(encrypted)
                .isRead(false)
                .build();

        ChatMessage saved = chatRepo.save(message);

        // Set plaintext để map ra response (không trả ciphertext cho client)
        saved.setMessageContent(req.getMessage());
        ChatMessageResponse response = mapper.toResponse(saved);

        // Push đến receiver: /topic/chat.{receiverId}
        messagingTemplate.convertAndSend(
                "/topic/chat." + req.getReceiverId().toString(),
                response
        );

        // Push echo về sender (đồng bộ đa tab): /topic/chat.{senderId}
        // Dùng type "ECHO" để frontend phân biệt, tránh duplicate optimistic message
        ChatMessageResponse echo = ChatMessageResponse.builder()
                .messageId(saved.getMessageId())
                .senderId(response.getSenderId())
                .senderName(response.getSenderName())
                .senderAvatar(response.getSenderAvatar())
                .receiverId(response.getReceiverId())
                .message(req.getMessage())
                .isRead(false)
                .createdAt(saved.getCreatedAt())
                .type("ECHO")
                .build();

        messagingTemplate.convertAndSend(
                "/topic/chat." + req.getSenderId().toString(),
                echo
        );

        log.info("Message saved & sent: {} -> {}", sender.getUsername(), receiver.getUsername());

        return response;
    }


    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(UUID user1, UUID user2) throws Exception {

        List<ChatMessage> messages = chatRepo.getConversation(user1, user2);
        List<ChatMessageResponse> result = new ArrayList<>();

        for (ChatMessage m : messages) {
            String plain = encryptionService.safeDecrypt(m.getMessageContent());
            if (plain == null) plain = ""; // null chỉ khi content trống
            m.setMessageContent(plain);
            result.add(mapper.toResponse(m));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyChats(UUID userId) {

        List<ChatMessage> latestMessages = chatRepo.findLatestConversations(userId);
        List<ConversationResponse> result = new ArrayList<>();

        for (ChatMessage m : latestMessages) {
            Users partner = m.getSender().getUserId().equals(userId)
                    ? m.getReceiver()
                    : m.getSender();

            String lastMsg = encryptionService.safeDecrypt(m.getMessageContent());
            if (lastMsg == null) lastMsg = ""; // sidebar chỉ hiện preview, để trống là ổn

            long unread = chatRepo.countUnread(partner.getUserId(), userId);

            result.add(ConversationResponse.builder()
                    .partnerId(partner.getUserId())
                    .partnerName(partner.getFullName())
                    .partnerAvatar(partner.getAvatarUrl())
                    .lastMessage(lastMsg)
                    .lastTime(m.getCreatedAt())
                    .unreadCount(unread)
                    .build());
        }

        return result;
    }

    @Transactional
    public void markAsRead(MarkReadRequest req) {
        int updated = chatRepo.markAsRead(req.getSenderId(), req.getReceiverId(), LocalDateTime.now());

        if (updated > 0) {
            ChatMessageResponse receipt = ChatMessageResponse.builder()
                    .senderId(req.getReceiverId())
                    .receiverId(req.getSenderId())
                    .type("READ_RECEIPT")
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/chat." + req.getSenderId().toString(),
                    receipt
            );
        }
    }
}