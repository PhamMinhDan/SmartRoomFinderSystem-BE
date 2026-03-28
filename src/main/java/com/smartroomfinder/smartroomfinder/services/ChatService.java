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
    private final NotificationService notificationService;
    private final EmailService emailService;


    @Transactional
    public ChatMessageResponse saveAndSend(ChatMessageRequest req) throws Exception {

        // ================= USER + ROOM =================
        Users sender = userRepo.findById(req.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + req.getSenderId()));

        Users receiver = userRepo.findById(req.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + req.getReceiverId()));

        Rooms room = roomRepo.findById(req.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found: " + req.getRoomId()));

        // ================= CHECK NOTIFY =================
        long total = chatRepo.countConversation(req.getSenderId(), req.getReceiverId());
        LocalDateTime lastTime = chatRepo.getLastMessageTime(req.getSenderId(), req.getReceiverId());

        boolean shouldNotify = false;
        if (total == 0) shouldNotify = true;
        if (lastTime != null && lastTime.isBefore(LocalDateTime.now().minusHours(30))) shouldNotify = true;
        if (total >= 30) shouldNotify = true;

        log.info("DEBUG NOTIFY → total={}, lastTime={}, shouldNotify={}", total, lastTime, shouldNotify);

        // ================= ENCRYPT + SAVE =================
        String plainText = req.getMessage();
        String encrypted = encryptionService.encrypt(plainText);

        ChatMessage message = ChatMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .room(room)
                .messageContent(encrypted)
                .isRead(false)
                .build();

        ChatMessage saved = chatRepo.save(message);

        ChatMessageResponse response = ChatMessageResponse.builder()
                .messageId(saved.getMessageId())
                .senderId(sender.getUserId())
                .senderName(sender.getFullName())
                .senderAvatar(sender.getAvatarUrl())
                .receiverId(receiver.getUserId())
                .message(plainText)
                .isRead(false)
                .createdAt(saved.getCreatedAt())
                .type("MESSAGE")
                .build();

        // ================= SOCKET → RECEIVER =================
        messagingTemplate.convertAndSend(
                "/topic/chat." + req.getReceiverId(),
                response
        );

        // ================= SOCKET → SENDER (ECHO) =================
        ChatMessageResponse echo = ChatMessageResponse.builder()
                .messageId(saved.getMessageId())
                .senderId(sender.getUserId())
                .senderName(sender.getFullName())
                .senderAvatar(sender.getAvatarUrl())
                .receiverId(receiver.getUserId())
                .message(plainText)
                .isRead(false)
                .createdAt(saved.getCreatedAt())
                .type("ECHO")
                .build();

        messagingTemplate.convertAndSend(
                "/topic/chat." + req.getSenderId(),
                echo
        );

        // ================= NOTIFICATION =================
        if (shouldNotify) {
            log.info("🔥 SEND NOTIFICATION → {}", receiver.getUsername());

            notificationService.createNotification(
                    req.getReceiverId(),
                    "Bạn có tin nhắn mới",
                    sender.getFullName() + " đã nhắn tin cho bạn",
                    "/chat?user=" + sender.getUserId()
            );

            emailService.sendChatNotification(
                    receiver.getEmail(),
                    sender.getFullName()
            );
        }

        return response;
    }


    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(UUID user1, UUID user2) throws Exception {

        List<ChatMessage> messages = chatRepo.getConversation(user1, user2);
        List<ChatMessageResponse> result = new ArrayList<>();

        for (ChatMessage m : messages) {
            String plain = encryptionService.safeDecrypt(m.getMessageContent());
            if (plain == null) plain = "";

            ChatMessageResponse resp = mapper.toResponse(m);
            resp.setMessage(plain);
            result.add(resp);
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
            if (lastMsg == null) lastMsg = "";

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