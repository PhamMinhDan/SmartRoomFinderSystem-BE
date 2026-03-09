package com.smartroomfinder.smartroomfinder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminRoomResponse {

    private Long roomId;
    private String title;
    private String description;
    private String address;
    private String cityName;
    private String districtName;
    private String wardName;
    private BigDecimal pricePerMonth;
    private BigDecimal depositAmount;
    private BigDecimal areaSize;
    private String furnishLevel;
    private String availabilityStatus;
    private Boolean isApproved;
    private Boolean isActive;
    private LocalDateTime createdAt;

    private String landlordId;
    private String landlordName;
    private String landlordEmail;
    private String landlordPhone;
    private String landlordAvatar;
    private Boolean landlordIdentityVerified;

    private PendingVerification pendingVerification;

    // Images
    private List<String> imageUrls;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PendingVerification {
        private Long verificationId;
        private String documentType;
        private String frontImageUrl;
        private String backImageUrl;
        private String selfieImageUrl;
        private String phoneNumber;
        private String status;
        private LocalDateTime createdAt;
    }
}