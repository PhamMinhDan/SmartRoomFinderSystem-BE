package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.request.CreateReportRequest;
import com.smartroomfinder.smartroomfinder.dto.request.ResolveReportRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ReportResponse;
import com.smartroomfinder.smartroomfinder.entities.RoomAddresses;
import com.smartroomfinder.smartroomfinder.entities.RoomReports;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.repositories.RoomReportRepository;
import com.smartroomfinder.smartroomfinder.repositories.RoomRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final RoomReportRepository reportRepository;
    private final RoomRepository       roomRepository;
    private final UserRepository       userRepository;

    private static final Map<String, String> REASON_LABELS = Map.of(
            "FRAUD",        "Lừa đảo",
            "DUPLICATE",    "Trùng lặp",
            "RENTED",       "Bất động sản đã cho thuê",
            "UNREACHABLE",  "Không liên lạc được",
            "WRONG_INFO",   "Thông tin bất động sản không đúng thực tế",
            "WRONG_POSTER", "Thông tin người đăng không đúng thực tế",
            "OTHER",        "Lý do khác"
    );

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "PENDING",   "Chờ xử lý",
            "RESOLVED",  "Đã xử lý",
            "DISMISSED", "Bỏ qua"
    );

    // ── User: gửi báo cáo ────────────────────────────────────────
    @Transactional
    public ReportResponse createReport(Long roomId, UUID reporterId, CreateReportRequest req) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        Users reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (reportRepository.existsByRoom_RoomIdAndReporter_UserId(roomId, reporterId)) {
            throw new IllegalStateException("Bạn đã báo cáo phòng này rồi");
        }

        RoomReports report = RoomReports.builder()
                .room(room)
                .reporter(reporter)
                .reason(req.getReason())
                .details(req.getDetails())
                .reporterPhone(req.getReporterPhone())
                .reporterEmail(req.getReporterEmail())
                .status("PENDING")
                .build();

        reportRepository.save(report);
        log.info("Report created: roomId={} reporterId={} reason={}", roomId, reporterId, req.getReason());

        return toResponse(report);
    }

    // ── Admin: lấy danh sách báo cáo ────────────────────────────
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RoomReports> reports = (status != null && !status.isBlank())
                ? reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        return reports.map(this::toResponse);
    }

    // ── Admin: lấy báo cáo theo phòng ───────────────────────────
    @Transactional(readOnly = true)
    public Page<ReportResponse> getReportsByRoom(Long roomId, int page, int size) {
        return reportRepository
                .findByRoomId(roomId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    // ── Admin: xử lý báo cáo ────────────────────────────────────
    @Transactional
    public ReportResponse resolveReport(Long reportId, UUID adminId, ResolveReportRequest req) {
        RoomReports report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));

        if (!"PENDING".equals(report.getStatus())) {
            throw new IllegalStateException("Báo cáo này đã được xử lý");
        }

        Users admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        report.setStatus(req.getStatus());
        report.setAdminNote(req.getAdminNote());
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());

        reportRepository.save(report);
        log.info("Report resolved: reportId={} status={} adminId={}", reportId, req.getStatus(), adminId);

        return toResponse(report);
    }

    // ── Mapper ───────────────────────────────────────────────────
    private ReportResponse toResponse(RoomReports r) {
        Rooms room = r.getRoom();

        // Lấy địa chỉ từ bảng room_addresses
        RoomAddresses addr = room.getRoomAddress();
        String roomAddress = addr != null
                ? addr.getStreetAddress() + ", " + addr.getDistrictName() + ", " + addr.getCityName()
                : "";

        String imageUrl = room.getImages().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .findFirst()
                .or(() -> room.getImages().stream().findFirst())
                .map(i -> i.getImageUrl())
                .orElse(null);

        return ReportResponse.builder()
                .reportId(r.getReportId())
                .roomId(room.getRoomId())
                .roomTitle(room.getTitle())
                .roomAddress(roomAddress)
                .roomImageUrl(imageUrl)
                .reporterName(r.getReporter().getFullName())
                .reporterEmail(r.getReporterEmail())
                .reporterPhone(r.getReporterPhone())
                .reason(r.getReason())
                .reasonLabel(REASON_LABELS.getOrDefault(r.getReason(), r.getReason()))
                .details(r.getDetails())
                .status(r.getStatus())
                .statusLabel(STATUS_LABELS.getOrDefault(r.getStatus(), r.getStatus()))
                .adminNote(r.getAdminNote())
                .resolvedByName(r.getResolvedBy() != null ? r.getResolvedBy().getFullName() : null)
                .resolvedAt(r.getResolvedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}