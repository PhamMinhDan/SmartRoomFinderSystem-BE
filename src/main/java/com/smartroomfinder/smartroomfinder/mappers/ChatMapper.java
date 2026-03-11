package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.ChatMessageResponse;
import com.smartroomfinder.smartroomfinder.entities.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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
    ChatMessageResponse toResponse(ChatMessage chatMessage);
}