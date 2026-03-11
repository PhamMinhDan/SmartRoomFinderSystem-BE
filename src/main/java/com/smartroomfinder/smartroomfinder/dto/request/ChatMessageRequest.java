package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ChatMessageRequest {

    @NotNull(message = "roomId không được null")
    private Long roomId;

    @NotNull(message = "senderId không được null")
    private UUID senderId;

    @NotNull(message = "receiverId không được null")
    private UUID receiverId;

    @NotBlank(message = "Tin nhắn không được rỗng")
    private String message;
}