package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
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
}