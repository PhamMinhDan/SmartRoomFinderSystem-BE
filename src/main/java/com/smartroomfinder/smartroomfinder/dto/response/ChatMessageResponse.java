package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ChatMessageResponse {

    private Long messageId;
    private UUID senderId;
    private String senderName;
    private String senderAvatar;
    private UUID receiverId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String type;

    @Builder.Default
    private List<AttachmentResponse> attachments = new ArrayList<>();

    @Builder.Default
    private Map<String, Long> reactions = new java.util.HashMap<>();

    private String myReaction;

    private Boolean recalledForAll;
    private Boolean recalledForSender;
}