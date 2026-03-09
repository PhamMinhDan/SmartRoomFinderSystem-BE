package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "identity_verifications",
        indexes = {
                @Index(name = "idx_iv_user_id", columnList = "user_id"),
                @Index(name = "idx_iv_status", columnList = "status")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IdentityVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long verificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // CCCD / Hộ chiếu / Bằng lái
    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "front_image_url", length = 500)
    private String frontImageUrl;

    @Column(name = "back_image_url", length = 500)
    private String backImageUrl;

    @Column(name = "selfie_image_url", length = 500)
    private String selfieImageUrl;

    // pending / approved / rejected
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "pending";

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}