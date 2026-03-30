package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatReactionRequest {

    @NotNull
    private Long messageId;

    @NotNull
    private UUID userId;

    @NotBlank
    private String emoji;
}