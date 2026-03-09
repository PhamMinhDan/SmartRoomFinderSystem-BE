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


    // ── Submit verification ───────────────────────────────────────
    @Transactional
    public IdentityVerificationResponse submitVerification(
            IdentityVerificationRequest req, UUID userId) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (user.getIdentityVerified()) {
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

    // ── Admin: Approve → mark verified + promote LANDLORD ─────────
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
        return mapper.toResponse(iv);
    }

    // ── Admin: Reject ─────────────────────────────────────────────
    @Transactional
    public IdentityVerificationResponse rejectVerification(Long verificationId, String reason) {
        IdentityVerification iv = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu xác minh"));

        iv.setStatus("rejected");
        iv.setRejectReason(reason);
        iv.setReviewedAt(LocalDateTime.now());
        verificationRepository.save(iv);

        log.info("Identity rejected - verificationId: {}", verificationId);
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

    // ── Lấy tiện ích đang active ────────────────────
    public List<AmenityResponse> getActive() {
        return amenityRepository.findByIsActiveTrue()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    // ── Lấy theo id ─────────────────────────────────
    public AmenityResponse getById(Long id) {
        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        return amenityMapper.toResponse(amenity);
    }

    // ── Tạo tiện ích mới ────────────────────────────
    public AmenityResponse create(AmenityRequest request) {

        if (amenityRepository.existsByAmenityNameIgnoreCase(request.getAmenityName())) {
            throw new RuntimeException("Amenity already exists");
        }

        Amenities entity = amenityMapper.toEntity(request);

        Amenities saved = amenityRepository.save(entity);

        return amenityMapper.toResponse(saved);
    }

    // ── Update tiện ích ─────────────────────────────
    public AmenityResponse update(Long id, AmenityRequest request) {

        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        amenityMapper.updateEntity(amenity, request);

        Amenities updated = amenityRepository.save(amenity);

        return amenityMapper.toResponse(updated);
    }

    // ── Xóa tiện ích ────────────────────────────────
    public void delete(Long id) {

        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        amenityRepository.delete(amenity);
    }
}