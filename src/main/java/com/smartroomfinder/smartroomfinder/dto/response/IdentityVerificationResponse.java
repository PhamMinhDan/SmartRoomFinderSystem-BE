package com.smartroomfinder.smartroomfinder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentityVerificationResponse {
    private Long verificationId;
    private Long userId;
    private String phoneNumber;
    private String documentType;
    private String frontImageUrl;
    private String backImageUrl;
    private String selfieImageUrl;
    private String status;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}