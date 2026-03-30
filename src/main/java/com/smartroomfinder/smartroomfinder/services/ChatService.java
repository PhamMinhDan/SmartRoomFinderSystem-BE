package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.request.AttachmentRequest;
import com.smartroomfinder.smartroomfinder.dto.request.ChatMessageRequest;
import com.smartroomfinder.smartroomfinder.dto.request.MarkReadRequest;
import com.smartroomfinder.smartroomfinder.dto.request.ChatReactionRequest;
import com.smartroomfinder.smartroomfinder.dto.request.ChatRecallRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AttachmentResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ChatMessageResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ConversationResponse;
import com.smartroomfinder.smartroomfinder.entities.*;
import com.smartroomfinder.smartroomfinder.mappers.ChatMapper;
import com.smartroomfinder.smartroomfinder.repositories.ChatReactionRepository;
import com.smartroomfinder.smartroomfinder.repositories.ChatRepository;
import com.smartroomfinder.smartroomfinder.repositories.RoomRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepo;
    private final ChatReactionRepository reactionRepo;
    private final UserRepository userRepo;
    private final RoomRepository roomRepo;
    private final EncryptionService encryptionService;
    private final ChatMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final EmailService emailService;

    // ── Gửi + lưu tin nhắn ──────────────────────────────────────────

    @Transactional
    public ChatMessageResponse saveAndSend(ChatMessageRequest req) throws Exception {

        Users sender = userRepo.findById(req.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found: " + req.getSenderId()));
        Users receiver = userRepo.findById(req.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + req.getReceiverId()));
        Rooms room = roomRepo.findById(req.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found: " + req.getRoomId()));

        boolean hasText = StringUtils.hasText(req.getMessage());
        boolean hasAttachments = req.getAttachments() != null && !req.getAttachments().isEmpty();

        if (!hasText && !hasAttachments)
            throw new IllegalArgumentException("Tin nhắn phải có nội dung hoặc file đính kèm");
        if (hasAttachments && req.getAttachments().size() > 5)
            throw new IllegalArgumentException("Tối đa 5 file mỗi lần gửi");

        long total = chatRepo.countConversation(req.getSenderId(), req.getReceiverId());
        LocalDateTime lastTime = chatRepo.getLastMessageTime(req.getSenderId(), req.getReceiverId());
        boolean shouldNotify = total == 0
                || (lastTime != null && lastTime.isBefore(LocalDateTime.now().minusHours(30)))
                || total >= 30;

        String plainText = hasText ? req.getMessage() : null;
        String encrypted = hasText ? encryptionService.encrypt(plainText) : null;

        ChatMessage message = ChatMessage.builder()
                .sender(sender).receiver(receiver).room(room)
                .messageContent(encrypted).isRead(false).build();

        if (hasAttachments) {
            List<AttachmentRequest> dtos = req.getAttachments();
            for (int i = 0; i < dtos.size(); i++) {
                AttachmentRequest dto = dtos.get(i);
                message.getAttachments().add(ChatAttachment.builder()
                        .message(message).fileUrl(dto.getFileUrl())
                        .fileType(dto.getFileType())
                        .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : i)
                        .build());
            }
        }

        ChatMessage saved = chatRepo.save(message);

        List<AttachmentResponse> attDtos = saved.getAttachments().stream()
                .map(mapper::toAttachmentResponse).toList();

        ChatMessageResponse response = buildResponse(saved, plainText, attDtos, "MESSAGE", sender, receiver, Map.of(), null);
        messagingTemplate.convertAndSend("/topic/chat." + req.getReceiverId(), response);

        ChatMessageResponse echo = buildResponse(saved, plainText, attDtos, "ECHO", sender, receiver, Map.of(), null);
        messagingTemplate.convertAndSend("/topic/chat." + req.getSenderId(), echo);

        if (shouldNotify) {
            notificationService.createNotification(req.getReceiverId(),
                    "Bạn có tin nhắn mới",
                    sender.getFullName() + " đã nhắn tin cho bạn",
                    "/chat?user=" + sender.getUserId());
            emailService.sendChatNotification(receiver.getEmail(), sender.getFullName());
        }

        return response;
    }

    private ChatMessageResponse buildResponse(ChatMessage saved, String plainText,
                                              List<AttachmentResponse> attDtos, String type,
                                              Users sender, Users receiver,
                                              Map<String, Long> reactions, String myReaction) {
        return ChatMessageResponse.builder()
                .messageId(saved.getMessageId())
                .senderId(sender.getUserId())
                .senderName(sender.getFullName())
                .senderAvatar(sender.getAvatarUrl())
                .receiverId(receiver.getUserId())
                .message(plainText)
                .isRead(false)
                .createdAt(saved.getCreatedAt())
                .type(type)
                .attachments(attDtos)
                .reactions(reactions)
                .myReaction(myReaction)
                .recalledForAll(saved.getRecalledForAll())
                .recalledForSender(saved.getRecalledForSender())
                .build();
    }

    // ── Lịch sử hội thoại ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(UUID user1, UUID user2) throws Exception {
        List<ChatMessage> messages = chatRepo.getConversationWithAttachments(user1, user2);
        if (messages.isEmpty()) return List.of();

        // Load tất cả reactions 1 lần (tránh N+1)
        List<Long> ids = messages.stream().map(ChatMessage::getMessageId).toList();
        List<ChatReaction> allReactions = reactionRepo.findAllByMessageIds(ids);
        Map<Long, List<ChatReaction>> reactionsByMsg = allReactions.stream()
                .collect(Collectors.groupingBy(r -> r.getMessage().getMessageId()));

        List<ChatMessageResponse> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            String plain = null;
            if (StringUtils.hasText(m.getMessageContent())) {
                plain = encryptionService.safeDecrypt(m.getMessageContent());
                if (plain == null) plain = "";
            }
            List<ChatReaction> msgReactions = reactionsByMsg.getOrDefault(m.getMessageId(), List.of());
            Map<String, Long> reactionMap = mapper.toReactionMap(msgReactions);
            String myReaction = mapper.findMyReaction(msgReactions, user1);

            ChatMessageResponse resp = mapper.toResponse(m);
            resp.setMessage(plain);
            resp.setReactions(reactionMap);
            resp.setMyReaction(myReaction);
            result.add(resp);
        }
        return result;
    }

    // ── Danh sách cuộc trò chuyện ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyChats(UUID userId) {
        List<ChatMessage> latestMessages = chatRepo.findLatestConversations(userId);
        List<ConversationResponse> result = new ArrayList<>();

        for (ChatMessage m : latestMessages) {
            Users partner = m.getSender().getUserId().equals(userId) ? m.getReceiver() : m.getSender();

            String lastMsg;
            if (m.getRecalledForAll()) {
                lastMsg = "Tin nhắn đã bị thu hồi";
            } else if (StringUtils.hasText(m.getMessageContent())) {
                lastMsg = encryptionService.safeDecrypt(m.getMessageContent());
                if (lastMsg == null) lastMsg = "";
            } else if (!m.getAttachments().isEmpty()) {
                boolean hasVideo = m.getAttachments().stream().anyMatch(a -> "VIDEO".equals(a.getFileType()));
                lastMsg = hasVideo ? "[Video]" : "[Hình ảnh]";
            } else {
                lastMsg = "";
            }

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

    // ── Mark as read ─────────────────────────────────────────────────

    @Transactional
    public void markAsRead(MarkReadRequest req) {
        int updated = chatRepo.markAsRead(req.getSenderId(), req.getReceiverId(), LocalDateTime.now());
        if (updated > 0) {
            ChatMessageResponse receipt = ChatMessageResponse.builder()
                    .senderId(req.getReceiverId()).receiverId(req.getSenderId())
                    .type("READ_RECEIPT").build();
            messagingTemplate.convertAndSend("/topic/chat." + req.getSenderId().toString(), receipt);
        }
    }

    // ── React / bỏ react ─────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse reactToMessage(ChatReactionRequest req) {
        ChatMessage message = chatRepo.findById(req.getMessageId())
                .orElseThrow(() -> new RuntimeException("Message not found: " + req.getMessageId()));
        Users user = userRepo.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + req.getUserId()));

        Optional<ChatReaction> existing = reactionRepo.findByMessage_MessageIdAndUser_UserId(
                req.getMessageId(), req.getUserId());

        if (existing.isPresent()) {
            if (existing.get().getEmoji().equals(req.getEmoji())) {
                // Click cùng emoji → bỏ react
                reactionRepo.delete(existing.get());
            } else {
                // Đổi sang emoji khác
                existing.get().setEmoji(req.getEmoji());
                reactionRepo.save(existing.get());
            }
        } else {
            reactionRepo.save(ChatReaction.builder()
                    .message(message).user(user).emoji(req.getEmoji()).build());
        }

        // Build và broadcast reaction update
        List<ChatReaction> allReactions = reactionRepo.findAllByMessage_MessageId(req.getMessageId());
        Map<String, Long> reactionMap = mapper.toReactionMap(allReactions);

        // Notify cả 2 phía
        UUID senderId = message.getSender().getUserId();
        UUID receiverId = message.getReceiver().getUserId();

        ChatMessageResponse update = ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(senderId).receiverId(receiverId)
                .type("REACTION_UPDATE")
                .reactions(reactionMap)
                .myReaction(mapper.findMyReaction(allReactions, senderId))
                .recalledForAll(message.getRecalledForAll())
                .recalledForSender(message.getRecalledForSender())
                .build();

        messagingTemplate.convertAndSend("/topic/chat." + senderId, update);

        ChatMessageResponse updateForReceiver = ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(senderId).receiverId(receiverId)
                .type("REACTION_UPDATE")
                .reactions(reactionMap)
                .myReaction(mapper.findMyReaction(allReactions, receiverId))
                .recalledForAll(message.getRecalledForAll())
                .recalledForSender(message.getRecalledForSender())
                .build();

        messagingTemplate.convertAndSend("/topic/chat." + receiverId, updateForReceiver);

        return update;
    }

    // ── Thu hồi tin nhắn ─────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse recallMessage(ChatRecallRequest req) {
        ChatMessage message = chatRepo.findById(req.getMessageId())
                .orElseThrow(() -> new RuntimeException("Message not found: " + req.getMessageId()));

        // Chỉ người gửi mới được thu hồi
        if (!message.getSender().getUserId().equals(req.getSenderId())) {
            throw new SecurityException("Bạn không có quyền thu hồi tin nhắn này");
        }

        if (req.getRecallForAll()) {
            message.setRecalledForAll(true);
        } else {
            message.setRecalledForSender(true);
        }
        message.setRecalledAt(LocalDateTime.now());
        chatRepo.save(message);

        UUID senderId = message.getSender().getUserId();
        UUID receiverId = message.getReceiver().getUserId();

        ChatMessageResponse recallEvent = ChatMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(senderId)
                .receiverId(receiverId)
                .type("RECALL")
                .recalledForAll(message.getRecalledForAll())
                .recalledForSender(message.getRecalledForSender())
                .build();

        messagingTemplate.convertAndSend("/topic/chat." + senderId, recallEvent);
        messagingTemplate.convertAndSend("/topic/chat." + receiverId, recallEvent);

        return recallEvent;
    }
}