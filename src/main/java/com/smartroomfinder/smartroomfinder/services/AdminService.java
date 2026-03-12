package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.response.AdminRoomResponse;
import com.smartroomfinder.smartroomfinder.dto.response.IdentityVerificationResponse;
import com.smartroomfinder.smartroomfinder.entities.*;
import com.smartroomfinder.smartroomfinder.mappers.IdentityVerificationMapper;
import com.smartroomfinder.smartroomfinder.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoomRepository roomRepository;
    private final IdentityVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final IdentityVerificationMapper verificationMapper;

    // ── Lấy danh sách phòng chờ duyệt ────────────────────────────
    @Transactional(readOnly = true)
    public Page<AdminRoomResponse> getPendingRooms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Rooms> rooms = roomRepository.findByIsApprovedFalseAndIsActiveTrue(pageable);
        return rooms.map(this::toAdminRoomResponse);
    }

    // ── Lấy tất cả phòng (có filter) ─────────────────────────────
    @Transactional(readOnly = true)
    public Page<AdminRoomResponse> getAllRooms(Boolean isApproved, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Rooms> rooms;
        if (isApproved != null) {
            rooms = roomRepository.findByIsApprovedAndIsActiveTrue(isApproved, pageable);
        } else {
            rooms = roomRepository.findByIsActiveTrue(pageable);
        }
        return rooms.map(this::toAdminRoomResponse);
    }

    // ── Chi tiết 1 phòng ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public AdminRoomResponse getRoomDetail(Long roomId) {
        Rooms room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        return toAdminRoomResponse(room);
    }

    // ── Duyệt phòng ───────────────────────────────────────────────
    @Transactional
    public AdminRoomResponse approveRoom(Long roomId) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        room.setIsApproved(true);
        room.setIsVerified(true);
        room.setDisplayUntil(LocalDateTime.now().plusDays(15));
        roomRepository.save(room);

        log.info("Room approved by admin - roomId: {}", roomId);
        return toAdminRoomResponse(room);
    }

    // ── Từ chối / ẩn phòng ────────────────────────────────────────
    @Transactional
    public AdminRoomResponse rejectRoom(Long roomId, String reason) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        room.setIsApproved(false);
        room.setIsActive(false);
        roomRepository.save(room);

        log.info("Room rejected by admin - roomId: {}, reason: {}", roomId, reason);
        return toAdminRoomResponse(room);
    }

    // ── Duyệt xác thực danh tính → promote LANDLORD ──────────────
    @Transactional
    public IdentityVerificationResponse approveVerification(Long verificationId) {
        IdentityVerification iv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu xác minh"));

        if (!"pending".equals(iv.getStatus())) {
            throw new IllegalStateException("Yêu cầu không ở trạng thái chờ duyệt");
        }

        Users user = iv.getUser();
        user.setIdentityVerified(true);
        user.setIdentityVerifiedAt(LocalDateTime.now());
        if (iv.getPhoneNumber() != null) user.setPhoneNumber(iv.getPhoneNumber());

        Roles landlordRole = roleRepository.findByRoleName("LANDLORD")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role LANDLORD"));
        user.setRole_id(landlordRole);
        userRepository.save(user);

        iv.setStatus("approved");
        iv.setReviewedAt(LocalDateTime.now());
        verificationRepository.save(iv);

        log.info("Verification approved - userId: {} promoted to LANDLORD", user.getUserId());
        return verificationMapper.toResponse(iv);
    }

    // ── Từ chối xác thực ─────────────────────────────────────────
    @Transactional
    public IdentityVerificationResponse rejectVerification(Long verificationId, String reason) {
        IdentityVerification iv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu xác minh"));

        iv.setStatus("rejected");
        iv.setRejectReason(reason);
        iv.setReviewedAt(LocalDateTime.now());
        verificationRepository.save(iv);

        log.info("Verification rejected - verificationId: {}", verificationId);
        return verificationMapper.toResponse(iv);
    }

    // ── Stats tổng quan ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public AdminStats getStats() {
        long totalRooms     = roomRepository.count();
        long pendingRooms   = roomRepository.countByIsApprovedFalseAndIsActiveTrue();
        long approvedRooms  = roomRepository.countByIsApprovedTrueAndIsActiveTrue();
        long pendingVerifs  = verificationRepository.countByStatus("pending");
        long totalUsers     = userRepository.count();

        return new AdminStats(totalRooms, pendingRooms, approvedRooms, pendingVerifs, totalUsers);
    }

    // ── Helper: convert Room → AdminRoomResponse ──────────────────
    private AdminRoomResponse toAdminRoomResponse(Rooms room) {
        Users landlord = room.getLandlord();

        // Check pending verification của landlord
        AdminRoomResponse.PendingVerification pendingVerif = null;
        if (!Boolean.TRUE.equals(landlord.getIdentityVerified())) {
            verificationRepository.findByUserAndStatus(landlord, "pending")
                    .ifPresent(iv -> {
                        // assigned below
                    });
            var ivOpt = verificationRepository.findByUserAndStatus(landlord, "pending");
            if (ivOpt.isPresent()) {
                IdentityVerification iv = ivOpt.get();
                pendingVerif = AdminRoomResponse.PendingVerification.builder()
                        .verificationId(iv.getVerificationId())
                        .documentType(iv.getDocumentType())
                        .frontImageUrl(iv.getFrontImageUrl())
                        .backImageUrl(iv.getBackImageUrl())
                        .selfieImageUrl(iv.getSelfieImageUrl())
                        .phoneNumber(iv.getPhoneNumber())
                        .status(iv.getStatus())
                        .createdAt(iv.getCreatedAt())
                        .build();
            }
        }

        List<String> imageUrls = room.getImages() == null ? List.of() :
                room.getImages().stream()
                        .sorted((a, b) -> Integer.compare(a.getImageOrder(), b.getImageOrder()))
                        .map(RoomImages::getImageUrl)
                        .toList();

        return AdminRoomResponse.builder()
                .roomId(room.getRoomId())
                .title(room.getTitle())
                .description(room.getDescription())
                .address(room.getAddress())
                .cityName(room.getCityName())
                .districtName(room.getDistrictName())
                .wardName(room.getWardName())
                .pricePerMonth(room.getPricePerMonth())
                .depositAmount(room.getDepositAmount())
                .areaSize(room.getAreaSize())
                .furnishLevel(room.getFurnishLevel())
                .availabilityStatus(room.getAvailabilityStatus())
                .isApproved(room.getIsApproved())
                .isActive(room.getIsActive())
                .createdAt(room.getCreatedAt())
                .landlordId(landlord.getUserId() != null ? landlord.getUserId().toString() : null)
                .landlordName(landlord.getFullName())
                .landlordEmail(landlord.getEmail())
                .landlordPhone(landlord.getPhoneNumber())
                .landlordAvatar(landlord.getAvatarUrl())
                .landlordIdentityVerified(landlord.getIdentityVerified())
                .pendingVerification(pendingVerif)
                .imageUrls(imageUrls)
                .build();
    }

    // ── Inner record cho stats ────────────────────────────────────
    public record AdminStats(
            long totalRooms,
            long pendingRooms,
            long approvedRooms,
            long pendingVerifications,
            long totalUsers
    ) {}
}