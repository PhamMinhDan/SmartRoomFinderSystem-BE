package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthGoogleResponse
{
    private String accessToken;
    private String refreshToken;
    private UserResponse user;
    private String message;
}
