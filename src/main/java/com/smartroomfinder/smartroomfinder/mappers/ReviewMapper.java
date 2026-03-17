package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.ReviewResponse;
import com.smartroomfinder.smartroomfinder.entities.Reviews;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "roomId", source = "room.roomId")
    @Mapping(target = "userId", expression = "java(review.getUser().getUserId().toString())")
    @Mapping(target = "userName", source = "user.fullName")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    @Mapping(target = "imageUrls", expression = "java(review.getImageUrlList())")
    ReviewResponse toResponse(Reviews review);
}