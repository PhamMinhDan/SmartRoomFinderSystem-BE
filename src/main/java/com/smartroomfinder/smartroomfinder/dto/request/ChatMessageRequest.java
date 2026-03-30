package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
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

    @Size(max = 5, message = "Tối đa 5 file mỗi lần gửi")
    private List<AttachmentRequest> attachments = new ArrayList<>();
}