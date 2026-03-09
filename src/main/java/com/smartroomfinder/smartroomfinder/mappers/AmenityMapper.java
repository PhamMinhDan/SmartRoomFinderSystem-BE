package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.request.AmenityRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AmenityResponse;
import com.smartroomfinder.smartroomfinder.entities.Amenities;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AmenityMapper {

    AmenityResponse toResponse(Amenities entity);

    Amenities toEntity(AmenityRequest request);

    void updateEntity(@MappingTarget Amenities entity, AmenityRequest request);
}