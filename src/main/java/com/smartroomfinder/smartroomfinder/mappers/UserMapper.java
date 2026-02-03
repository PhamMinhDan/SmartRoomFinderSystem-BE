package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.UserResponse;
import com.smartroomfinder.smartroomfinder.entities.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role_id.roleName", target = "roleName")
    UserResponse toResponse(Users user);
}
