package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.IdentityVerificationResponse;
import com.smartroomfinder.smartroomfinder.entities.IdentityVerification;
import com.smartroomfinder.smartroomfinder.entities.Users;
import org.springframework.stereotype.Component;

@Component
public class IdentityVerificationMapper {

    public IdentityVerificationResponse toResponse(IdentityVerification iv) {
        if (iv == null) return null;

        Users user = iv.getUser();
        Long userId = null;
        if (user != null && user.getUserId() != null) {
            userId = user.getUserId().getLeastSignificantBits()
                    ^ user.getUserId().getMostSignificantBits();
        }

        return IdentityVerificationResponse.builder()
                .verificationId(iv.getVerificationId())
                .userId(userId)
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .email(user.getEmail())
                .phoneNumber(iv.getPhoneNumber())
                .documentType(iv.getDocumentType())
                .frontImageUrl(iv.getFrontImageUrl())
                .backImageUrl(iv.getBackImageUrl())
                .selfieImageUrl(iv.getSelfieImageUrl())
                .status(iv.getStatus())
                .rejectReason(iv.getRejectReason())
                .createdAt(iv.getCreatedAt())
                .reviewedAt(iv.getReviewedAt())
                .build();
    }
}