package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_amenities",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_room_amenity",
                columnNames = {"room_id", "amenity_id"}
        ),
        indexes = @Index(name = "idx_amenity_id", columnList = "amenity_id")
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoomAmenities {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_amenity_id")
    private Long roomAmenityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Rooms room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amenity_id", nullable = false)
    private Amenities amenity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}