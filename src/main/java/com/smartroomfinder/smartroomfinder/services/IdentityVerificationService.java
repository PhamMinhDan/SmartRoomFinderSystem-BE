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
import org.springframework.beans.factory.annotation.Value;
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
    private final NotificationService notificationService;
    private final EncryptionService encryptionService;

    @Value("${app.frontend-url}")
    private String frontendBase;

    // ── Submit verification → mã hóa dữ liệu nhạy cảm trước khi lưu ─────────
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
            try {
                user.setPhoneNumber(req.getPhoneNumber());
            } catch (Exception e) {
                log.error("Failed to encrypt phone for user {}: {}", userId, e.getMessage());
                user.setPhoneNumber(req.getPhoneNumber());
            }
            userRepository.save(user);
        }

        String encryptedPhone      = safeEncrypt(req.getPhoneNumber());
        String encryptedFrontUrl   = safeEncrypt(req.getFrontImageUrl());
        String encryptedBackUrl    = safeEncrypt(req.getBackImageUrl());
        String encryptedSelfieUrl  = req.getSelfieImageUrl() != null
                ? safeEncrypt(req.getSelfieImageUrl())
                : null;

        IdentityVerification iv = IdentityVerification.builder()
                .user(user)
                .phoneNumber(encryptedPhone)
                .documentType(req.getDocumentType())
                .frontImageUrl(encryptedFrontUrl)
                .backImageUrl(encryptedBackUrl)
                .selfieImageUrl(encryptedSelfieUrl)
                .status("pending")
                .build();

        IdentityVerification saved = verificationRepository.save(iv);
        log.info("Identity verification submitted (encrypted) - userId: {}", userId);

        // ── Notify + Email tất cả admin ───────────────────────────
        String userName = user.getFullName() != null ? user.getFullName() : user.getEmail();
        String notiTitle = "Yêu cầu xác thực danh tính mới";
        String notiContent = userName + " vừa gửi yêu cầu xác thực danh tính. Vui lòng kiểm duyệt.";
        String adminVerifyUrl = frontendBase + "/admin/pending-posts";

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

        // Trả về response đã giải mã (người dùng thấy thông tin gốc của họ)
        return toDecryptedResponse(saved);
    }

    // ── Lấy trạng thái xác thực của user hiện tại ────────────────
    @Transactional(readOnly = true)
    public IdentityVerificationResponse getMyVerification(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return verificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(this::toDecryptedResponse)
                .orElse(null);
    }

    // ── Admin: Approve → giải mã, cập nhật user, notify ──────────
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

        if (iv.getPhoneNumber() != null) {
            user.setPhoneNumber(encryptionService.safeDecrypt(iv.getPhoneNumber()));
        }

        Roles landlordRole = roleRepository.findByRoleName("LANDLORD")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role LANDLORD"));
        user.setRole_id(landlordRole);
        userRepository.save(user);

        iv.setStatus("approved");
        iv.setReviewedAt(LocalDateTime.now());
        verificationRepository.save(iv);

        log.info("Identity approved - userId: {}, promoted to LANDLORD", user.getUserId());

        notificationService.createNotification(
                user.getUserId(),
                "Xác thực danh tính thành công ",
                "Tài khoản của bạn đã được xác minh danh tính. Bạn có thể đăng tin cho thuê phòng ngay bây giờ!",
                frontendBase + "/post-room"
        );
        notificationService.sendEmail(
                user.getEmail(),
                "[SmartRoomFinder] Xác thực danh tính thành công",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Chúc mừng! Tài khoản của bạn đã được xác minh danh tính thành công.\n"
                        + "Bạn đã được nâng lên cấp độ Chủ nhà (LANDLORD) và có thể đăng tin cho thuê phòng.\n\n"
                        + "Truy cập ngay: " + frontendBase + "/post-room\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

        return toDecryptedResponse(iv);
    }

    // ── Admin: Reject → notify ────────────────────────────────────
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

        notificationService.createNotification(
                user.getUserId(),
                "Xác thực danh tính bị từ chối ",
                "Yêu cầu xác thực của bạn đã bị từ chối. Lý do: " + reason + ". Vui lòng thử lại.",
                frontendBase + "/verify-identity"
        );
        notificationService.sendEmail(
                user.getEmail(),
                "[SmartRoomFinder] Yêu cầu xác thực danh tính bị từ chối",
                "Xin chào " + user.getFullName() + ",\n\n"
                        + "Yêu cầu xác thực danh tính của bạn đã bị từ chối.\n"
                        + "Lý do: " + reason + "\n\n"
                        + "Vui lòng kiểm tra lại hồ sơ và gửi lại yêu cầu:\n"
                        + frontendBase + "/verify-identity\n\n"
                        + "Trân trọng,\nSmartRoomFinder"
        );

        return toDecryptedResponse(iv);
    }

    // ── Promote to LANDLORD nếu đã verified ──────────────────────
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

    // ── Amenity helpers (giữ nguyên) ──────────────────────────────
    public List<AmenityResponse> getAll() {
        return amenityRepository.findAll().stream().map(amenityMapper::toResponse).toList();
    }

    public List<AmenityResponse> getActive() {
        return amenityRepository.findByIsActiveTrue().stream().map(amenityMapper::toResponse).toList();
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
        return amenityMapper.toResponse(amenityRepository.save(amenityMapper.toEntity(request)));
    }

    public AmenityResponse update(Long id, AmenityRequest request) {
        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));
        amenityMapper.updateEntity(amenity, request);
        return amenityMapper.toResponse(amenityRepository.save(amenity));
    }

    public void delete(Long id) {
        amenityRepository.delete(amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found")));
    }

    // ── Private helpers ───────────────────────────────────────────

    public IdentityVerificationResponse toDecryptedResponse(IdentityVerification iv) {
        IdentityVerificationResponse resp = mapper.toResponse(iv);

        // Giải mã các field nhạy cảm trước khi trả về cho client
        resp.setPhoneNumber(encryptionService.safeDecrypt(iv.getPhoneNumber()));
        resp.setFrontImageUrl(encryptionService.safeDecrypt(iv.getFrontImageUrl()));
        resp.setBackImageUrl(encryptionService.safeDecrypt(iv.getBackImageUrl()));
        if (iv.getSelfieImageUrl() != null) {
            resp.setSelfieImageUrl(encryptionService.safeDecrypt(iv.getSelfieImageUrl()));
        }

        return resp;
    }

    private String safeEncrypt(String value) {
        if (value == null || value.isBlank()) return value;
        try {
            return encryptionService.encrypt(value);
        } catch (Exception e) {
            log.warn("Encryption failed, storing plaintext. Error: {}", e.getMessage());
            return value;
        }
    }
}