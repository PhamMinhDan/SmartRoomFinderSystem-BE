package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportResponse {

    private Long reportId;

    // Room info
    private Long roomId;
    private String roomTitle;
    private String roomAddress;
    private String roomImageUrl;

    // Reporter info
    private String reporterName;
    private String reporterEmail;
    private String reporterPhone;

    // Report info
    private String reason;
    private String reasonLabel;   // Hiển thị tiếng Việt
    private String details;
    private String status;
    private String statusLabel;   // Hiển thị tiếng Việt
    private String adminNote;

    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}