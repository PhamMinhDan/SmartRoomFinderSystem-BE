package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.request.CreateRoomRequest;
import com.smartroomfinder.smartroomfinder.dto.response.RoomResponse;
import com.smartroomfinder.smartroomfinder.entities.*;
import com.smartroomfinder.smartroomfinder.mappers.RoomMapper;
import com.smartroomfinder.smartroomfinder.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository        roomRepository;
    private final RoomAddressRepository roomAddressRepository;
    private final AmenityRepository     amenityRepository;
    private final UserRepository        userRepository;
    private final RoomMapper            roomMapper;
    private final MapboxService         mapboxService;

    // ── Create ────────────────────────────────────────────────────
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest req, UUID userId) {
        Users landlord = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Geocode từ địa chỉ đầy đủ
        String fullAddress = req.getStreetAddress() + ", "
                + req.getWardName()     + ", "
                + req.getDistrictName() + ", "
                + req.getCityName()     + ", Vietnam";
        BigDecimal[] latLng = mapboxService.geocode(fullAddress);

        // 1. Lưu Rooms (không còn address inline)
        Rooms room = Rooms.builder()
                .landlord(landlord)
                .title(req.getTitle())
                .description(req.getDescription())
                .areaSize(req.getAreaSize())
                .pricePerMonth(req.getPricePerMonth())
                .depositAmount(req.getDepositAmount())
                .capacity(req.getCapacity() != null ? req.getCapacity() : 1)
                .roomType(req.getRoomType())
                .furnishLevel(req.getFurnishLevel())
                .availableFrom(req.getAvailableFrom())
                .displayUntil(LocalDateTime.now().plusDays(15))
                .build();

        Rooms saved = roomRepository.save(room);

        // 2. Lưu RoomAddresses (1-1)
        RoomAddresses addr = RoomAddresses.builder()
                .room(saved)
                .streetAddress(req.getStreetAddress())
                .cityName(req.getCityName())
                .districtName(req.getDistrictName())
                .wardName(req.getWardName())
                .latitude(latLng[0])
                .longitude(latLng[1])
                .build();

        roomAddressRepository.save(addr);
        saved.setRoomAddress(addr);

        // 3. Images
        if (req.getMediaUrls() != null) {
            for (int i = 0; i < req.getMediaUrls().size(); i++) {
                saved.getImages().add(RoomImages.builder()
                        .room(saved)
                        .imageUrl(req.getMediaUrls().get(i))
                        .imageOrder(i)
                        .isPrimary(i == 0)
                        .uploadedBy(landlord)
                        .build());
            }
        }

        // 4. Amenities
        if (req.getAmenityIds() != null && !req.getAmenityIds().isEmpty()) {
            Set<Long> uniqueIds = new HashSet<>(req.getAmenityIds());
            amenityRepository.findByAmenityIdIn(uniqueIds).forEach(a ->
                    saved.getAmenities().add(RoomAmenities.builder()
                            .room(saved).amenity(a).build())
            );
        }

        Rooms result = roomRepository.save(saved);
        log.info("Room created - roomId: {}, userId: {}", result.getRoomId(), userId);
        return roomMapper.toResponse(result);
    }

    // ── Read ──────────────────────────────────────────────────────
    @Transactional
    public RoomResponse getRoomById(Long roomId) {
        Rooms room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        room.setViewCount(room.getViewCount() != null ? room.getViewCount() + 1 : 1);
        return roomMapper.toResponse(room);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getMyRooms(UUID userId, int page, int size) {
        Users landlord = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return roomRepository.findByLandlord(landlord, pageable)
                .map(roomMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getApprovedRooms(String city, String district, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return roomRepository.findApprovedRooms(city, district, pageable)
                .map(roomMapper::toResponse);
    }

    // ── Update ────────────────────────────────────────────────────
    @Transactional
    public RoomResponse updateRoom(Long roomId, CreateRoomRequest req, UUID userId) {
        Rooms room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getLandlord().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa phòng này");
        }

        // Cập nhật thông tin phòng
        room.setTitle(req.getTitle());
        room.setDescription(req.getDescription());
        room.setAreaSize(req.getAreaSize());
        room.setPricePerMonth(req.getPricePerMonth());
        room.setDepositAmount(req.getDepositAmount());
        if (req.getCapacity() != null) room.setCapacity(req.getCapacity());
        room.setRoomType(req.getRoomType());
        room.setFurnishLevel(req.getFurnishLevel());
        room.setAvailableFrom(req.getAvailableFrom());

        // Cập nhật địa chỉ trong bảng room_addresses + re-geocode
        RoomAddresses addr = room.getRoomAddress();
        if (addr == null) {
            addr = new RoomAddresses();
            addr.setRoom(room);
        }
        addr.setStreetAddress(req.getStreetAddress());
        addr.setCityName(req.getCityName());
        addr.setDistrictName(req.getDistrictName());
        addr.setWardName(req.getWardName());

        String fullAddress = req.getStreetAddress() + ", "
                + req.getWardName()     + ", "
                + req.getDistrictName() + ", "
                + req.getCityName()     + ", Vietnam";
        BigDecimal[] latLng = mapboxService.geocode(fullAddress);
        addr.setLatitude(latLng[0]);
        addr.setLongitude(latLng[1]);

        roomAddressRepository.save(addr);
        room.setRoomAddress(addr);

        // Cập nhật images
        if (req.getMediaUrls() != null) {
            room.getImages().clear();
            for (int i = 0; i < req.getMediaUrls().size(); i++) {
                room.getImages().add(RoomImages.builder()
                        .room(room)
                        .imageUrl(req.getMediaUrls().get(i))
                        .imageOrder(i)
                        .isPrimary(i == 0)
                        .uploadedBy(room.getLandlord())
                        .build());
            }
        }

        // Cập nhật amenities
        if (req.getAmenityIds() != null) {
            room.getAmenities().clear();
            roomRepository.saveAndFlush(room);

            Set<Long> uniqueIds = new HashSet<>(req.getAmenityIds());
            amenityRepository.findByAmenityIdIn(uniqueIds).forEach(a ->
                    room.getAmenities().add(RoomAmenities.builder()
                            .room(room).amenity(a).build())
            );
        }

        return roomMapper.toResponse(roomRepository.save(room));
    }

    // ── Delete (soft) ─────────────────────────────────────────────
    @Transactional
    public void deleteRoom(Long roomId, UUID userId) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getLandlord().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa phòng này");
        }

        room.setIsActive(false);
        roomRepository.save(room);
        log.info("Room soft-deleted - roomId: {}", roomId);
    }

    // ── Toggle isActive (ẩn/hiện tin) ────────────────────────────
    @Transactional
    public RoomResponse setRoomActive(Long roomId, UUID userId, boolean isActive, String reason) {
        Rooms room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getLandlord().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền thay đổi trạng thái phòng này");
        }

        room.setIsActive(isActive);

        if (!isActive) {
            room.setHiddenReason(reason);
            room.setHiddenAt(LocalDateTime.now());
            room.setAvailabilityStatus("hidden");
        } else {
            room.setHiddenReason(null);
            room.setHiddenAt(null);
            room.setAvailabilityStatus("available");
        }

        log.info("Room isActive={} - roomId: {}, userId: {}", isActive, roomId, userId);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    // ── Featured ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<RoomResponse> getFeaturedRooms(int page, int size) {
        return roomRepository.findFeaturedRooms(PageRequest.of(page, size))
                .map(roomMapper::toResponse);
    }

    // ── Search ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<RoomResponse> searchRooms(
            String city, String district, String roomType,
            BigDecimal priceMin, BigDecimal priceMax,
            BigDecimal areaMin,  BigDecimal areaMax,
            Double minRating,    List<String> amenities,
            int page, int size,  String sort
    ) {
        Sort sorting = switch (sort) {
            case "price_asc"  -> Sort.by("pricePerMonth").ascending();
            case "price_desc" -> Sort.by("pricePerMonth").descending();
            default           -> Sort.by("createdAt").descending();
        };

        return roomRepository.searchRooms(
                city, district, roomType,
                priceMin, priceMax,
                areaMin,  areaMax,
                minRating,
                (amenities == null || amenities.isEmpty()) ? null : amenities,
                PageRequest.of(page, size, sorting)
        ).map(roomMapper::toResponse);
    }

    // ── Extend ────────────────────────────────────────────────────
    @Transactional
    public RoomResponse extendRoom(Long roomId, UUID userId, int days) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getLandlord().getUserId().equals(userId)) {
            throw new AccessDeniedException("Không có quyền");
        }

        LocalDateTime now = LocalDateTime.now();
        room.setDisplayUntil(
                room.getDisplayUntil() == null || room.getDisplayUntil().isBefore(now)
                        ? now.plusDays(days)
                        : room.getDisplayUntil().plusDays(days)
        );

        return roomMapper.toResponse(roomRepository.save(room));
    }
}