package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {

    private UUID partnerId;

    private String partnerName;

    private String partnerAvatar;

    private String lastMessage;

    private LocalDateTime lastTime;

    private long unreadCount;
}