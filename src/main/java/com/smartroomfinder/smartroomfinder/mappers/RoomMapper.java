package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.AmenityResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomImageResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomResponse;
import com.smartroomfinder.smartroomfinder.entities.RoomAmenities;
import com.smartroomfinder.smartroomfinder.entities.RoomImages;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoomMapper {

    public RoomResponse toResponse(Rooms room) {
        if (room == null) return null;

        List<RoomImageResponse> images = room.getImages() == null ? List.of() :
                room.getImages().stream()
                        .sorted((a, b) -> Integer.compare(a.getImageOrder(), b.getImageOrder()))
                        .map(this::toImageResponse)
                        .toList();

        List<AmenityResponse> amenities = room.getAmenities() == null ? List.of() :
                room.getAmenities().stream()
                        .map(this::toAmenityResponse)
                        .toList();

        return RoomResponse.builder()
                .roomId(room.getRoomId())
                .title(room.getTitle())
                .description(room.getDescription())
                .address(room.getAddress())
                .cityName(room.getCityName())
                .districtName(room.getDistrictName())
                .wardName(room.getWardName())
                .latitude(room.getLatitude())
                .longitude(room.getLongitude())
                .areaSize(room.getAreaSize())
                .pricePerMonth(room.getPricePerMonth())
                .depositAmount(room.getDepositAmount())
                .capacity(room.getCapacity())
                .roomType(room.getRoomType())
                .furnishLevel(room.getFurnishLevel())
                .availableFrom(room.getAvailableFrom())
                .availabilityStatus(room.getAvailabilityStatus())
                .isVerified(room.getIsVerified())
                .isApproved(room.getIsApproved())
                .isActive(room.getIsActive())
                .viewCount(room.getViewCount())
                .averageRating(room.getAverageRating())
                .totalReviews(room.getTotalReviews())
                .landlordId(room.getLandlord().getUserId() != null
                        ? room.getLandlord().getUserId().getMostSignificantBits() : null)
                .landlordName(room.getLandlord().getFullName())
                .landlordAvatar(room.getLandlord().getAvatarUrl())
                .images(images)
                .amenities(amenities)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private RoomImageResponse toImageResponse(RoomImages img) {
        return RoomImageResponse.builder()
                .imageId(img.getImageId())
                .imageUrl(img.getImageUrl())
                .imageOrder(img.getImageOrder())
                .isPrimary(img.getIsPrimary())
                .build();
    }

    private AmenityResponse toAmenityResponse(RoomAmenities ra) {
        return AmenityResponse.builder()
                .amenityId(ra.getAmenity().getAmenityId())
                .amenityName(ra.getAmenity().getAmenityName())
                .iconUrl(ra.getAmenity().getIconUrl())
                .category(ra.getAmenity().getCategory())
                .build();
    }

    public List<RoomResponse> toResponseList(List<Rooms> rooms) {
        return rooms.stream().map(this::toResponse).toList();
    }
}