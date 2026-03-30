package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatRecallRequest {

    @NotNull
    private Long messageId;

    @NotNull
    private UUID senderId;

    @NotNull
    private Boolean recallForAll;
}