package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.AmenityResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomAddressResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomImageResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomResponse;
import com.smartroomfinder.smartroomfinder.entities.RoomAddresses;
import com.smartroomfinder.smartroomfinder.entities.RoomAmenities;
import com.smartroomfinder.smartroomfinder.entities.RoomImages;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    // ── Rooms → RoomResponse ──────────────────────────────────────
    @Mapping(target = "address",        source = "roomAddress")
    @Mapping(target = "landlordId",     expression = "java(room.getLandlord().getUserId() != null ? room.getLandlord().getUserId().toString() : null)")
    @Mapping(target = "landlordName",   source = "landlord.fullName")
    @Mapping(target = "landlordAvatar", source = "landlord.avatarUrl")
    @Mapping(target = "phoneNumber",    source = "landlord.phoneNumber")
    @Mapping(target = "expiredAt",      source = "displayUntil")
    @Mapping(target = "images",         expression = "java(mapImages(room.getImages()))")
    @Mapping(target = "amenities",      expression = "java(mapAmenities(room.getAmenities()))")
    RoomResponse toResponse(Rooms room);

    List<RoomResponse> toResponseList(List<Rooms> rooms);

    // ── RoomAddresses → RoomAddressResponse ──────────────────────
    RoomAddressResponse toAddressResponse(RoomAddresses addr);

    // ── RoomImages → RoomImageResponse ────────────────────────────
    RoomImageResponse toImageResponse(RoomImages img);

    // ── RoomAmenities → AmenityResponse ──────────────────────────
    @Mapping(target = "amenityId",   source = "amenity.amenityId")
    @Mapping(target = "amenityName", source = "amenity.amenityName")
    @Mapping(target = "iconUrl",     source = "amenity.iconUrl")
    @Mapping(target = "category",    source = "amenity.category")
    AmenityResponse toAmenityResponse(RoomAmenities ra);

    // ── Sort helpers (default method – MapStruct giữ nguyên) ─────
    default List<RoomImageResponse> mapImages(Set<RoomImages> images) {
        if (images == null) return List.of();
        return images.stream()
                .sorted(Comparator.comparingInt(RoomImages::getImageOrder))
                .map(this::toImageResponse)
                .toList();
    }

    default List<AmenityResponse> mapAmenities(Set<RoomAmenities> amenities) {
        if (amenities == null) return List.of();
        return amenities.stream()
                .sorted(Comparator.comparing(a -> a.getAmenity().getAmenityName()))
                .map(this::toAmenityResponse)
                .toList();
    }
}