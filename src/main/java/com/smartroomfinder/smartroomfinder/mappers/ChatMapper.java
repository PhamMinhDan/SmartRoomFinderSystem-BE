package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.AttachmentResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ChatMessageResponse;
import com.smartroomfinder.smartroomfinder.entities.ChatAttachment;
import com.smartroomfinder.smartroomfinder.entities.ChatMessage;
import com.smartroomfinder.smartroomfinder.entities.ChatReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(target = "messageId",    source = "messageId")
    @Mapping(target = "senderId",     source = "sender.userId")
    @Mapping(target = "senderName",   source = "sender.fullName")
    @Mapping(target = "senderAvatar", source = "sender.avatarUrl")
    @Mapping(target = "receiverId",   source = "receiver.userId")
    @Mapping(target = "message",      source = "messageContent")   // ← key mapping
    @Mapping(target = "isRead",       source = "isRead")
    @Mapping(target = "createdAt",    source = "createdAt")
    @Mapping(target = "type",         constant = "MESSAGE")
    @Mapping(target = "attachments", source = "attachments")
    ChatMessageResponse toResponse(ChatMessage chatMessage);

    @Mapping(target = "attachmentId", source = "attachmentId")
    @Mapping(target = "fileUrl",      source = "fileUrl")
    @Mapping(target = "fileType",     source = "fileType")
    @Mapping(target = "sortOrder",    source = "sortOrder")
    AttachmentResponse toAttachmentResponse(ChatAttachment attachment);

    // ── Helper: build reaction map ────────────────────────────────
    default Map<String, Long> toReactionMap(List<ChatReaction> reactions) {
        if (reactions == null || reactions.isEmpty()) return Map.of();
        return reactions.stream()
                .collect(Collectors.groupingBy(ChatReaction::getEmoji, Collectors.counting()));
    }

    default String findMyReaction(List<ChatReaction> reactions, UUID myUserId) {
        if (reactions == null || myUserId == null) return null;
        return reactions.stream()
                .filter(r -> r.getUser().getUserId().equals(myUserId))
                .map(ChatReaction::getEmoji)
                .findFirst()
                .orElse(null);
    }
}