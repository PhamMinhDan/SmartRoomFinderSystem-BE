package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.request.AmenityRequest;
import com.smartroomfinder.smartroomfinder.dto.request.IdentityVerificationRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AmenityResponse;
import com.smartroomfinder.smartroomfinder.dto.response.IdentityVerificationResponse;
import com.smartroomfinder.smartroomfinder.entities.*;
import com.smartroomfinder.smartroomfinder.mappers.AmenityMapper;
import com.smartroomfinder.smartroomfinder.mappers.IdentityVerificationMapper;
import com.smartroomfinder.smartroomfinder.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final IdentityVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AmenityRepository amenityRepository;
    private final IdentityVerificationMapper mapper;
    private final AmenityMapper amenityMapper;
    private final NotificationService notificationService;   // ← inject

    private static final String ADMIN_URL = "http://localhost:4200/admin/pending-posts";

    // ── Submit verification → notify + email admin ────────────────
    @Transactional
    public IdentityVerificationResponse submitVerification(
            IdentityVerificationRequest req, UUID userId) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (Boolean.TRUE.equals(user.getIdentityVerified())) {
            throw new IllegalStateException("Tài khoản đã được xác minh danh tính");
        }

        verificationRepository.findByUserAndStatus(user, "pending").ifPresent(existing -> {
            throw new IllegalStateException("Yêu cầu xác minh đang chờ duyệt");
        });

        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(req.getPhoneNumber());
            userRepository.save(user);
        }

        IdentityVerification iv = IdentityVerification.builder()
                .user(user)
                .phoneNumber(req.getPhoneNumber())
                .documentType(req.getDocumentType())
                .frontImageUrl(req.getFrontImageUrl())
                .backImageUrl(req.getBackImageUrl())
                .selfieImageUrl(req.getSelfieImageUrl())
                .status("pending")
                .build();

        IdentityVerification saved = verificationRepository.save(iv);
        log.info("Identity verification submitted - userId: {}", userId);

        // ── Notify + Email tất cả admin ───────────────────────────
        String userName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String notiTitle = "Yêu cầu xác thực danh tính mới";
        String notiContent = userName + " vừa gửi yêu cầu xác thực danh tính. Vui lòng kiểm duyệt.";
        String adminVerifyUrl = "http://localhost:4200/admin/pending-posts"; // tab verifications nếu có

        notificationService.notifyAllAdmins(notiTitle, notiContent, adminVerifyUrl, "VERIFICATION");

        notificationService.sendEmailToAllAdmins(
                "[SmartRoomFinder] Yêu cầu xác thực danh tính mới",
                "Xin chào Admin,\n\n"
                        + userName + " (" + user.getEmail() + ") vừa gửi yêu cầu xác thực danh tính.\n"
                        + "Loại giấy tờ: " + req.getDocumentType() + "\n"
                        + "SĐT: " + req.getPhoneNumber() + "\n\n"
                        + "Truy cập trang quản trị để kiểm duyệt:\n"
                        + adminVerifyUrl + "\n\n"
                        + "Trân trọng,\nSmartRoomFinder System"
        );

        return mapper.toResponse(saved);
    }

    // ── Get current status ────────────────────────────────────────
    @Transactional(readOnly = true)
    public IdentityVerificationResponse getMyVerification(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return verificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(mapper::toResponse)
                .orElse(null);
    }

    // ── Admin: Approve → notify + email user ──────────────────────
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

        log.info("Identity approved - userId: {}, promoted to LANDLORD", user.getUserId());

        // ── Notify + Email user ────────────────────────────────────
        notificationService.createNotification(
                user.getUserId(),
                "Xác thực danh tính thành công ",
                "Tài khoản của bạn đã được xác minh danh tính. Bạn có thể đăng tin cho thuê phòng ngay bây giờ!",
                "http://localhost:4200/post-room"
        );

        notificationService.sendEmail(
                user.getEmail(),
                "[SmartRoomFinder] Xác thực danh tính thành công",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Chúc mừng! Tài khoản của bạn đã được xác minh danh tính thành công.\n"
                        + "Bạn đã được nâng lên cấp độ Chủ nhà (LANDLORD) và có thể đăng tin cho thuê phòng.\n\n"
                        + "Truy cập ngay: http://localhost:4200/post-room\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

        return mapper.toResponse(iv);
    }

    // ── Admin: Reject → notify + email user ──────────────────────
    @Transactional
    public IdentityVerificationResponse rejectVerification(Long verificationId, String reason) {
        IdentityVerification iv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu xác minh"));

        iv.setStatus("rejected");
        iv.setRejectReason(reason);
        iv.setReviewedAt(LocalDateTime.now());
        verificationRepository.save(iv);

        log.info("Identity rejected - verificationId: {}", verificationId);

        Users user = iv.getUser();

        // ── Notify + Email user ────────────────────────────────────
        notificationService.createNotification(
                user.getUserId(),
                "Xác thực danh tính bị từ chối ",
                "Yêu cầu xác thực của bạn đã bị từ chối. Lý do: " + reason + ". Vui lòng thử lại.",
                "http://localhost:4200/verify-identity"
        );

        notificationService.sendEmail(
                user.getEmail(),
                "[SmartRoomFinder] Yêu cầu xác thực danh tính bị từ chối",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Yêu cầu xác thực danh tính của bạn đã bị từ chối.\n"
                        + "Lý do: " + reason + "\n\n"
                        + "Vui lòng kiểm tra lại hồ sơ và gửi lại yêu cầu:\n"
                        + "http://localhost:4200/verify-identity\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

        return mapper.toResponse(iv);
    }

    @Transactional
    public void promoteToLandlordIfVerified(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        boolean isAlreadyLandlord = user.getRole_id() != null
                && "LANDLORD".equals(user.getRole_id().getRoleName());

        if (!isAlreadyLandlord && Boolean.TRUE.equals(user.getIdentityVerified())) {
            Roles landlordRole = roleRepository.findByRoleName("LANDLORD")
                    .orElseThrow(() -> new RuntimeException("Role LANDLORD không tồn tại"));
            user.setRole_id(landlordRole);
            userRepository.save(user);
            log.info("User {} promoted to LANDLORD after posting room", userId);
        }
    }

    public List<AmenityResponse> getAll() {
        return amenityRepository.findAll()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    public List<AmenityResponse> getActive() {
        return amenityRepository.findByIsActiveTrue()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    public AmenityResponse getById(Long id) {
        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));
        return amenityMapper.toResponse(amenity);
    }

    public AmenityResponse create(AmenityRequest request) {
        if (amenityRepository.existsByAmenityNameIgnoreCase(request.getAmenityName())) {
            throw new RuntimeException("Amenity already exists");
        }
        Amenities entity = amenityMapper.toEntity(request);
        return amenityMapper.toResponse(amenityRepository.save(entity));
    }

    public AmenityResponse update(Long id, AmenityRequest request) {
        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));
        amenityMapper.updateEntity(amenity, request);
        return amenityMapper.toResponse(amenityRepository.save(amenity));
    }

    public void delete(Long id) {
        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));
        amenityRepository.delete(amenity);
    }
}