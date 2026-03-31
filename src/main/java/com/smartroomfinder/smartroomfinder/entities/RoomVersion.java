package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "room_edit_requests",
        indexes = {
                @Index(name = "idx_rer_room_id",  columnList = "room_id"),
                @Index(name = "idx_rer_status",   columnList = "status"),
                @Index(name = "idx_rer_created",  columnList = "created_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoomVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "edit_request_id")
    private Long versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Rooms room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private Users requestedBy;

    @Column(name = "new_data", nullable = false, columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String newData;

    @Column(name = "old_data", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String oldData;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Users reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}