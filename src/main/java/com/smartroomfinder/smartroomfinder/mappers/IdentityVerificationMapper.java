package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.IdentityVerificationResponse;
import com.smartroomfinder.smartroomfinder.entities.IdentityVerification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface IdentityVerificationMapper {

    @Mapping(target = "userId", source = "user", qualifiedByName = "mapUserId")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatarUrl", source = "user.avatarUrl")
    @Mapping(target = "email", source = "user.email")
    IdentityVerificationResponse toResponse(IdentityVerification iv);

    @Named("mapUserId")
    default Long mapUserId(com.smartroomfinder.smartroomfinder.entities.Users user) {
        if (user == null || user.getUserId() == null) return null;

        return user.getUserId().getLeastSignificantBits()
                ^ user.getUserId().getMostSignificantBits();
    }
}