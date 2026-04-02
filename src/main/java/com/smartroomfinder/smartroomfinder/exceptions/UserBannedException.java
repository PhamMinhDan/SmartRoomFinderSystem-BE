package com.smartroomfinder.smartroomfinder.exceptions;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserBannedException extends RuntimeException {

    private final String banReason;
    private final LocalDateTime bannedAt;

    public UserBannedException(String banReason, LocalDateTime bannedAt) {
        super("USER_BANNED");
        this.banReason = banReason;
        this.bannedAt = bannedAt;
    }
}