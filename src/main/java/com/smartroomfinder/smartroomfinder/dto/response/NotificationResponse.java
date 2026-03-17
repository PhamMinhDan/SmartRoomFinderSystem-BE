package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String content;
    private String redirectUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
