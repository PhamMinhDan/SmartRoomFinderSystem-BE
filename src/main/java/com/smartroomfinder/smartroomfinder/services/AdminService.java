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
    private final NotificationService notificationService;

    private static final String FRONTEND_BASE = "http://localhost:4200";

    // ── Lấy danh sách phòng chờ duyệt ────────────────────────────
    @Transactional(readOnly = true)
    public Page<AdminRoomResponse> getPendingRooms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return roomRepository.findByIsApprovedFalseAndIsActiveTrue(pageable)
                .map(this::toAdminRoomResponse);
    }

    // ── Lấy tất cả phòng (có filter) ─────────────────────────────
    @Transactional(readOnly = true)
    public Page<AdminRoomResponse> getAllRooms(Boolean isApproved, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Rooms> rooms = isApproved != null
                ? roomRepository.findByIsApprovedAndIsActiveTrue(isApproved, pageable)
                : roomRepository.findByIsActiveTrue(pageable);
        return rooms.map(this::toAdminRoomResponse);
    }

    // ── Chi tiết 1 phòng ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public AdminRoomResponse getRoomDetail(Long roomId) {
        Rooms room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        return toAdminRoomResponse(room);
    }

    // ── Lấy danh sách yêu cầu xác thực ───────────────────────────
    @Transactional(readOnly = true)
    public Page<IdentityVerificationResponse> getVerifications(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<IdentityVerification> verifications = (status != null && !status.isBlank())
                ? verificationRepository.findByStatus(status, pageable)
                : verificationRepository.findAll(pageable);
        return verifications.map(verificationMapper::toResponse);
    }

    // ── Duyệt phòng ───────────────────────────────────────────────
    @Transactional
    public AdminRoomResponse approveRoom(Long roomId) {
        Rooms room = roomRepository.findByIdWithDetails(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        Users landlord = room.getLandlord();

        if (!Boolean.TRUE.equals(landlord.getIdentityVerified())) {
            throw new IllegalStateException(
                    "LANDLORD_NOT_VERIFIED: Người đăng chưa xác thực danh tính. Vui lòng duyệt xác thực trước.");
        }

        room.setIsApproved(true);
        room.setIsVerified(true);
        room.setDisplayUntil(LocalDateTime.now().plusDays(15));
        roomRepository.save(room);

        log.info("Room approved by admin - roomId: {}", roomId);

        // Lấy địa chỉ từ room_addresses
        RoomAddresses addr = room.getRoomAddress();
        String addressLine = addr != null
                ? addr.getStreetAddress() + ", " + addr.getDistrictName() + ", " + addr.getCityName()
                : "";

        String roomUrl = FRONTEND_BASE + "/rooms/" + roomId;

        notificationService.createNotification(
                landlord.getUserId(),
                "Tin đăng đã được duyệt ",
                "Tin đăng \"" + room.getTitle() + "\" của bạn đã được admin phê duyệt và hiển thị trên hệ thống.",
                roomUrl
        );

        notificationService.sendEmail(
                landlord.getEmail(),
                "[SmartRoomFinder] Tin đăng của bạn đã được duyệt",
                "Xin chào " + landlord.getFullName() + ",\n\n"
                        + "Tin đăng của bạn đã được admin phê duyệt thành công!\n\n"
                        + "📌 Tiêu đề: " + room.getTitle() + "\n"
                        + "📍 Địa chỉ: " + addressLine + "\n"
                        + "💰 Giá: " + room.getPricePerMonth() + " đ/tháng\n\n"
                        + "Xem tin đăng của bạn tại:\n" + roomUrl + "\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

        return toAdminRoomResponse(room);
    }

    // ── Từ chối phòng ─────────────────────────────────────────────
    @Transactional
    public AdminRoomResponse rejectRoom(Long roomId, String reason) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        room.setIsApproved(false);
        room.setIsActive(false);
        roomRepository.save(room);

        log.info("Room rejected by admin - roomId: {}, reason: {}", roomId, reason);

        Users landlord = room.getLandlord();
        String rejectReason = (reason != null && !reason.isBlank()) ? reason : "Không đạt yêu cầu kiểm duyệt";

        notificationService.createNotification(
                landlord.getUserId(),
                "Tin đăng bị từ chối ",
                "Tin đăng \"" + room.getTitle() + "\" đã bị từ chối. Lý do: " + rejectReason,
                FRONTEND_BASE + "/my-posts"
        );

        notificationService.sendEmail(
                landlord.getEmail(),
                "[SmartRoomFinder] Tin đăng của bạn bị từ chối",
                "Xin chào " + landlord.getFullName() + ",\n\n"
                        + "Rất tiếc, tin đăng của bạn đã bị từ chối kiểm duyệt.\n\n"
                        + "📌 Tiêu đề: " + room.getTitle() + "\n"
                        + "❌ Lý do: " + rejectReason + "\n\n"
                        + "Vui lòng chỉnh sửa và đăng lại:\n"
                        + FRONTEND_BASE + "/post-room\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

        return toAdminRoomResponse(room);
    }

    // ── Duyệt xác thực danh tính ─────────────────────────────────
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

        notificationService.createNotification(
                user.getUserId(),
                "Xác thực danh tính thành công ",
                "Tài khoản của bạn đã được xác minh. Bạn có thể đăng tin cho thuê phòng ngay bây giờ!",
                FRONTEND_BASE + "/post-room"
        );

        notificationService.sendEmail(
                user.getEmail(),
                "[SmartRoomFinder] Xác thực danh tính thành công",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Chúc mừng! Tài khoản của bạn đã được xác minh danh tính thành công.\n"
                        + "Bạn đã được nâng lên cấp độ Chủ nhà (LANDLORD).\n\n"
                        + "Đăng tin ngay tại: " + FRONTEND_BASE + "/post-room\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

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

        Users user = iv.getUser();

        notificationService.createNotification(
                user.getUserId(),
                "Xác thực danh tính bị từ chối ",
                "Yêu cầu xác thực bị từ chối. Lý do: " + reason + ". Vui lòng thử lại.",
                FRONTEND_BASE + "/verify-identity"
        );

        notificationService.sendEmail(
                user.getEmail(),
                "[SmartRoomFinder] Yêu cầu xác thực danh tính bị từ chối",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Yêu cầu xác thực danh tính của bạn bị từ chối.\n"
                        + "Lý do: " + reason + "\n\n"
                        + "Vui lòng thử lại tại: " + FRONTEND_BASE + "/verify-identity\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

        return verificationMapper.toResponse(iv);
    }

    // ── Stats tổng quan ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public AdminStats getStats() {
        return new AdminStats(
                roomRepository.count(),
                roomRepository.countByIsApprovedFalseAndIsActiveTrue(),
                roomRepository.countByIsApprovedTrueAndIsActiveTrue(),
                verificationRepository.countByStatus("pending"),
                userRepository.count()
        );
    }

    // ── Helper: Rooms → AdminRoomResponse ─────────────────────────
    private AdminRoomResponse toAdminRoomResponse(Rooms room) {
        Users landlord = room.getLandlord();

        // Lấy địa chỉ từ bảng room_addresses
        RoomAddresses addr = room.getRoomAddress();

        AdminRoomResponse.PendingVerification pendingVerif = null;
        if (!Boolean.TRUE.equals(landlord.getIdentityVerified())) {
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
                // địa chỉ từ room_addresses
                .address(addr != null ? addr.getStreetAddress() : null)
                .cityName(addr != null ? addr.getCityName() : null)
                .districtName(addr != null ? addr.getDistrictName() : null)
                .wardName(addr != null ? addr.getWardName() : null)
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

    public record AdminStats(
            long totalRooms,
            long pendingRooms,
            long approvedRooms,
            long pendingVerifications,
            long totalUsers
    ) {}
}