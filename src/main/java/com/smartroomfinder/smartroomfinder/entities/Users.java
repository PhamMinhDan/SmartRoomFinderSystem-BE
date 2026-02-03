package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_email", columnList = "email"),
                @Index(name = "idx_username", columnList = "username"),
                @Index(name = "idx_role_id", columnList = "role_id"),
                @Index(name = "idx_is_active", columnList = "is_active"),
                @Index(name = "idx_google_id", columnList = "google_id"),
                @Index(name = "idx_provider", columnList = "auth_provider")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID userId;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Roles role_id;

    @Version
    @Column(name = "version")
    private Long version;

    // ===== Basic User Info =====
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Addresses address;

    // ===== Identity Verification =====
    @Column(name = "identity_card", unique = true, length = 20)
    private String identityCard;

    @Column(name = "identity_verified", nullable = false)
    private Boolean identityVerified = false;

    @Column(name = "identity_verified_at")
    private LocalDateTime identityVerifiedAt;

    // ===== Account Status =====
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;

    @Column(name = "ban_reason", length = 500)
    private String banReason;

    @Column(name = "banned_at")
    private LocalDateTime bannedAt;

    // ===== Token & Authentication Fields =====
    @Column(name = "access_token", columnDefinition = "LONGTEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "LONGTEXT")
    private String refreshToken;

    @Column(name = "access_token_expires_at")
    private LocalDateTime accessTokenExpiresAt;

    @Column(name = "refresh_token_expires_at")
    private LocalDateTime refreshTokenExpiresAt;

    // ===== OAuth Provider Fields =====
    @Column(name = "auth_provider", length = 50)
    private String authProvider; // GOOGLE, FACEBOOK, LOCAL

    @Column(name = "google_id", length = 255, unique = true)
    private String googleId;

    @Column(name = "facebook_id", length = 255, unique = true)
    private String facebookId;

    @Column(name = "is_oauth_user")
    private Boolean isOAuthUser = false;

    @Column(name = "oauth_email_verified")
    private Boolean oauthEmailVerified = false;

    // ===== Login Tracking =====
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // ===== Audit Fields =====
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}