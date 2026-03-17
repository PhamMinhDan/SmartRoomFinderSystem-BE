package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "room_reports",
    indexes = {
        @Index(name = "idx_report_room_id",     columnList = "room_id"),
        @Index(name = "idx_report_reporter_id", columnList = "reporter_id"),
        @Index(name = "idx_report_status",      columnList = "status"),
        @Index(name = "idx_report_created_at",  columnList = "created_at")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoomReports {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    // Phòng bị báo cáo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Rooms room;

    // Người báo cáo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Users reporter;

    // Lý do: FRAUD | DUPLICATE | RENTED | UNREACHABLE | WRONG_INFO | WRONG_POSTER | OTHER
    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "reporter_phone", length = 20)
    private String reporterPhone;

    @Column(name = "reporter_email", length = 255)
    private String reporterEmail;

    // PENDING | RESOLVED | DISMISSED
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    // Admin xử lý
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Users resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}