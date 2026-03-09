package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.AddressResponse;
import com.smartroomfinder.smartroomfinder.entities.Addresses;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressResponse toResponse(Addresses address);
}