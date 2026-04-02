package com.smartroomfinder.smartroomfinder.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartroomfinder.smartroomfinder.dto.request.CreateRoomRequest;
import com.smartroomfinder.smartroomfinder.dto.response.RoomVersionResponse;
import com.smartroomfinder.smartroomfinder.entities.*;
import com.smartroomfinder.smartroomfinder.mappers.RoomVersionMapper;
import com.smartroomfinder.smartroomfinder.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomVersionService {

    private final RoomVersionRepository VersionRepository;
    private final RoomRepository roomRepository;
    private final RoomAddressRepository roomAddressRepository;
    private final AmenityRepository amenityRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final RoomVersionMapper roomVersionMapper;
    
    @Value("${app.frontend-url")
    private String frontendBase;


    @Transactional
    public RoomVersionResponse submitVersion(Long roomId, CreateRoomRequest req, UUID userId) {

        Rooms room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        room.setIsActive(false);
        room.setHiddenReason("Thông tin phòng đang chờ phê duyệt");
        room.setHiddenAt(LocalDateTime.now());

        if (!room.getLandlord().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa phòng này");
        }

        // Không cho gửi 2 request PENDING cùng lúc cho 1 phòng
        if (VersionRepository.existsByRoomAndStatus(room, "PENDING")) {
            throw new IllegalStateException("Phòng này đang có yêu cầu chỉnh sửa chờ duyệt. Vui lòng chờ admin phê duyệt.");
        }

        Users landlord = room.getLandlord();

        // ── Snapshot dữ liệu CŨ ──────────────────────────────────
        RoomAddresses oldAddr = room.getRoomAddress();
        Map<String, Object> oldMap = new LinkedHashMap<>();
        oldMap.put("title",           room.getTitle());
        oldMap.put("description",     room.getDescription());
        oldMap.put("pricePerMonth",   room.getPricePerMonth());
        oldMap.put("depositAmount",   room.getDepositAmount());
        oldMap.put("areaSize",        room.getAreaSize());
        oldMap.put("capacity",        room.getCapacity());
        oldMap.put("roomType",        room.getRoomType());
        oldMap.put("furnishLevel",    room.getFurnishLevel());
        oldMap.put("availableFrom",   room.getAvailableFrom() != null ? room.getAvailableFrom().toString() : null);
        oldMap.put("streetAddress",   oldAddr != null ? oldAddr.getStreetAddress() : null);
        oldMap.put("wardName",        oldAddr != null ? oldAddr.getWardName() : null);
        oldMap.put("districtName",    oldAddr != null ? oldAddr.getDistrictName() : null);
        oldMap.put("cityName",        oldAddr != null ? oldAddr.getCityName() : null);
        oldMap.put("mediaUrls",       room.getImages() == null ? List.of() :
                room.getImages().stream()
                        .sorted(Comparator.comparingInt(RoomImages::getImageOrder))
                        .map(RoomImages::getImageUrl).toList());
        oldMap.put("amenityIds",      room.getAmenities() == null ? List.of() :
                room.getAmenities().stream()
                        .map(ra -> ra.getAmenity().getAmenityId()).toList());
        oldMap.put("amenityNames",    room.getAmenities() == null ? List.of() :
                room.getAmenities().stream()
                        .map(ra -> ra.getAmenity().getAmenityName()).toList());

        // ── Dữ liệu MỚI ──────────────────────────────────────────
        Map<String, Object> newMap = new LinkedHashMap<>();
        newMap.put("title",           req.getTitle());
        newMap.put("description",     req.getDescription());
        newMap.put("pricePerMonth",   req.getPricePerMonth());
        newMap.put("depositAmount",   req.getDepositAmount());
        newMap.put("areaSize",        req.getAreaSize());
        newMap.put("capacity",        req.getCapacity());
        newMap.put("roomType",        req.getRoomType());
        newMap.put("furnishLevel",    req.getFurnishLevel());
        newMap.put("availableFrom",   req.getAvailableFrom() != null ? req.getAvailableFrom().toString() : null);
        newMap.put("streetAddress",   req.getStreetAddress());
        newMap.put("wardName",        req.getWardName());
        newMap.put("districtName",    req.getDistrictName());
        newMap.put("cityName",        req.getCityName());
        newMap.put("mediaUrls",       req.getMediaUrls() != null ? req.getMediaUrls() : List.of());
        newMap.put("amenityIds",      req.getAmenityIds() != null ? req.getAmenityIds() : List.of());

        // Lấy tên tiện ích để admin đọc dễ hơn
        if (req.getAmenityIds() != null && !req.getAmenityIds().isEmpty()) {
            List<String> names = amenityRepository
                    .findByAmenityIdIn(new HashSet<>(req.getAmenityIds()))
                    .stream().map(Amenities::getAmenityName).toList();
            newMap.put("amenityNames", names);
        } else {
            newMap.put("amenityNames", List.of());
        }

        try {
            String oldJson = objectMapper.writeValueAsString(oldMap);
            String newJson = objectMapper.writeValueAsString(newMap);

            RoomVersion Version = RoomVersion.builder()
                    .room(room)
                    .requestedBy(landlord)
                    .oldData(oldJson)
                    .newData(newJson)
                    .status("PENDING")
                    .build();

            RoomVersion saved = VersionRepository.save(Version);
            notifyAdmins(room, landlord, saved.getVersionId());

            return roomVersionMapper.toResponse(saved);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu yêu cầu chỉnh sửa: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Admin lấy danh sách edit request (theo status)
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<RoomVersionResponse> getVersions(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String statusFilter = (status == null || status.isBlank()) ? "PENDING" : status.toUpperCase();
        return VersionRepository
                .findByStatusOrderByCreatedAtDesc(statusFilter, pageable)
                .map(roomVersionMapper::toResponse);
    }

    // ─────────────────────────────────────────────────────────────
    // Admin lấy chi tiết 1 edit request
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public RoomVersionResponse getVersionDetail(Long VersionId) {
        RoomVersion req = VersionRepository.findById(VersionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chỉnh sửa"));
        return roomVersionMapper.toResponse(req);
    }

    // ─────────────────────────────────────────────────────────────
    // Admin DUYỆT edit request → áp dụng newData vào rooms
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public RoomVersionResponse approveVersion(Long VersionId, UUID adminUserId) {
        RoomVersion editReq = VersionRepository.findById(VersionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chỉnh sửa"));

        if (!"PENDING".equals(editReq.getStatus())) {
            throw new IllegalStateException("Yêu cầu không ở trạng thái PENDING");
        }

        Users admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        try {
            // Parse newData JSON
            Map<?, ?> newMap = objectMapper.readValue(editReq.getNewData(), Map.class);
            Rooms room = editReq.getRoom();
            Rooms fullRoom = roomRepository.findByIdWithDetails(room.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

            // Áp dụng các field cơ bản
            applyStringField(newMap, "title",        fullRoom::setTitle);
            applyStringField(newMap, "description",  fullRoom::setDescription);
            applyStringField(newMap, "roomType",     fullRoom::setRoomType);
            applyStringField(newMap, "furnishLevel",  fullRoom::setFurnishLevel);

            if (newMap.get("pricePerMonth") != null)
                fullRoom.setPricePerMonth(new java.math.BigDecimal(newMap.get("pricePerMonth").toString()));
            if (newMap.get("depositAmount") != null)
                fullRoom.setDepositAmount(new java.math.BigDecimal(newMap.get("depositAmount").toString()));
            if (newMap.get("areaSize") != null)
                fullRoom.setAreaSize(new java.math.BigDecimal(newMap.get("areaSize").toString()));
            if (newMap.get("capacity") != null)
                fullRoom.setCapacity(Integer.parseInt(newMap.get("capacity").toString()));
            if (newMap.get("availableFrom") != null && !newMap.get("availableFrom").toString().isBlank())
                fullRoom.setAvailableFrom(java.time.LocalDate.parse(newMap.get("availableFrom").toString()));

            // Địa chỉ
            RoomAddresses addr = fullRoom.getRoomAddress();
            if (addr == null) { addr = new RoomAddresses(); addr.setRoom(fullRoom); }
            applyStringField(newMap, "streetAddress", addr::setStreetAddress);
            applyStringField(newMap, "wardName",      addr::setWardName);
            applyStringField(newMap, "districtName",  addr::setDistrictName);
            applyStringField(newMap, "cityName",      addr::setCityName);
            roomAddressRepository.save(addr);
            fullRoom.setRoomAddress(addr);

            // Images
            @SuppressWarnings("unchecked")
            List<String> mediaUrls = (List<String>) newMap.get("mediaUrls");
            if (mediaUrls != null) {
                fullRoom.getImages().clear();
                for (int i = 0; i < mediaUrls.size(); i++) {
                    fullRoom.getImages().add(RoomImages.builder()
                            .room(fullRoom)
                            .imageUrl(mediaUrls.get(i))
                            .imageOrder(i)
                            .isPrimary(i == 0)
                            .uploadedBy(fullRoom.getLandlord())
                            .build());
                }
            }

            // Amenities
            @SuppressWarnings("unchecked")
            List<?> amenityIdRaw = (List<?>) newMap.get("amenityIds");
            if (amenityIdRaw != null) {
                fullRoom.getAmenities().clear();
                roomRepository.saveAndFlush(fullRoom);
                Set<Long> ids = amenityIdRaw.stream()
                        .map(o -> Long.parseLong(o.toString()))
                        .collect(Collectors.toSet());
                if (!ids.isEmpty()) {
                    amenityRepository.findByAmenityIdIn(ids).forEach(a ->
                            fullRoom.getAmenities().add(
                                    RoomAmenities.builder().room(fullRoom).amenity(a).build()));
                }
            }

            fullRoom.setRejectedByAdmin(false);
            fullRoom.setHiddenReason(null);
            fullRoom.setIsActive(true);
            fullRoom.setHiddenAt(null);
            roomRepository.save(fullRoom);

            // Cập nhật edit request
            editReq.setStatus("APPROVED");
            editReq.setReviewedBy(admin);
            editReq.setReviewedAt(LocalDateTime.now());
            VersionRepository.save(editReq);

            log.info("Version approved - VersionId: {}, roomId: {}", VersionId, fullRoom.getRoomId());

            // Thông báo landlord
            Users landlord = editReq.getRequestedBy();
            notificationService.createNotification(
                    landlord.getUserId(),
                    "Yêu cầu chỉnh sửa được duyệt",
                    "Yêu cầu chỉnh sửa phòng \"" + fullRoom.getTitle() + "\" đã được admin phê duyệt.",
                    frontendBase + "/my-posts"
            );
            notificationService.sendEmail(
                    landlord.getEmail(),
                    "[SmartRoomFinder] Yêu cầu chỉnh sửa phòng được chấp thuận",
                    "Xin chào " + landlord.getFullName() + ",\n\n"
                            + "Yêu cầu chỉnh sửa thông tin phòng \"" + fullRoom.getTitle() + "\" của bạn đã được admin phê duyệt.\n"
                            + "Tin đăng đã được cập nhật hiển thị.\n\n"
                            + "Xem tin tại: " + frontendBase + "/my-posts\n\n"
                            + "Trân trọng,\nSmartRoomFinder"
            );

            return roomVersionMapper.toResponse(editReq);

        } catch (IllegalStateException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi áp dụng chỉnh sửa: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Admin TỪ CHỐI edit request
    // ─────────────────────────────────────────────────────────────
    @Transactional
    public RoomVersionResponse rejectVersion(Long VersionId, String reason, UUID adminUserId) {
        RoomVersion editReq = VersionRepository.findById(VersionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chỉnh sửa"));

        if (!"PENDING".equals(editReq.getStatus())) {
            throw new IllegalStateException("Yêu cầu không ở trạng thái PENDING");
        }

        Users admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        String rejectReason = (reason != null && !reason.isBlank()) ? reason : "Nội dung chỉnh sửa không phù hợp";

        editReq.setStatus("REJECTED");
        editReq.setRejectReason(rejectReason);
        editReq.setReviewedBy(admin);
        editReq.setReviewedAt(LocalDateTime.now());
        VersionRepository.save(editReq);

        log.info("Version rejected - VersionId: {}", VersionId);

        Users landlord = editReq.getRequestedBy();
        Rooms room = editReq.getRoom();

        notificationService.createNotification(
                landlord.getUserId(),
                "Yêu cầu chỉnh sửa bị từ chối ",
                "Yêu cầu chỉnh sửa phòng \"" + room.getTitle() + "\" bị từ chối. Lý do: " + rejectReason,
                frontendBase + "/my-posts"
        );
        notificationService.sendEmail(
                landlord.getEmail(),
                "[SmartRoomFinder] Yêu cầu chỉnh sửa phòng bị từ chối",
                "Xin chào " + landlord.getFullName() + ",\n\n"
                        + "Yêu cầu chỉnh sửa phòng \"" + room.getTitle() + "\" của bạn đã bị từ chối.\n"
                        + "Lý do: " + rejectReason + "\n\n"
                        + "Bạn có thể gửi lại yêu cầu chỉnh sửa sau khi cập nhật nội dung phù hợp.\n\n"
                        + "Xem tin tại: " + frontendBase + "/my-posts\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

        return roomVersionMapper.toResponse(editReq);
    }

    // ─────────────────────────────────────────────────────────────
    // Landlord kiểm tra trạng thái pending edit của 1 phòng
    // ─────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Optional<RoomVersionResponse> getPendingEditForRoom(Long roomId) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        return VersionRepository
                .findTopByRoomAndStatusOrderByCreatedAtDesc(room, "PENDING")
                .map(roomVersionMapper::toResponse);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────
    private void notifyAdmins(Rooms room, Users landlord, Long VersionId) {
        // Thông báo hệ thống cho tất cả ADMIN
        userRepository.findAllByRoleName("ADMIN").forEach(admin ->
                notificationService.createNotification(
                        admin.getUserId(),
                        "Yêu cầu chỉnh sửa phòng mới 📝",
                        "Landlord " + landlord.getFullName() + " gửi yêu cầu chỉnh sửa phòng \""
                                + room.getTitle() + "\". Vui lòng kiểm tra và phê duyệt.",
                        frontendBase + "/admin/pending-posts"
                )
        );

        // Gửi email cho admin (nếu có danh sách)
        userRepository.findAllByRoleName("ADMIN").forEach(admin ->
                notificationService.sendEmail(
                        admin.getEmail(),
                        "[SmartRoomFinder] Yêu cầu chỉnh sửa phòng mới",
                        "Xin chào " + admin.getFullName() + ",\n\n"
                                + "Landlord " + landlord.getFullName() + " (" + landlord.getEmail() + ") vừa gửi yêu cầu chỉnh sửa phòng:\n"
                                + " Tiêu đề: " + room.getTitle() + "\n"
                                + " Edit Request ID: " + VersionId + "\n\n"
                                + "Truy cập trang quản trị để phê duyệt:\n"
                                + frontendBase + "/admin/pending-posts\n\n"
                                + "Trân trọng,\nSmartRoomFinder System"
                )
        );
    }

    private void applyStringField(Map<?, ?> map, String key, Consumer<String> setter) {
        Object val = map.get(key);
        if (val != null) setter.accept(val.toString());
    }

}