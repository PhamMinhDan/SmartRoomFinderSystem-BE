package com.smartroomfinder.smartroomfinder.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("bio")
    private String bio;

    // Address info - nested object
    @JsonProperty("address")
    private AddressResponse address;

    @JsonProperty("identity_verified")
    private Boolean identityVerified;

    @JsonProperty("role_name")
    private String roleName;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_banned")
    private Boolean isBanned;

    @JsonProperty("auth_provider")
    private String authProvider;

    @JsonProperty("is_oauth_user")
    private Boolean isOAuthUser;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("last_login")
    private LocalDateTime lastLogin;
}