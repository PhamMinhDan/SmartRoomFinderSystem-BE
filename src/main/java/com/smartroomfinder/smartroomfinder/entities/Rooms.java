package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
        name = "rooms",
        indexes = {
                @Index(name = "idx_landlord_id", columnList = "landlord_id"),
                @Index(name = "idx_city", columnList = "city_name"),
                @Index(name = "idx_district", columnList = "district_name"),
                @Index(name = "idx_price", columnList = "price_per_month"),
                @Index(name = "idx_availability", columnList = "availability_status"),
                @Index(name = "idx_is_approved", columnList = "is_approved"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Rooms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private Users landlord;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ── Address ──────────────────────────────────────────────────
    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Column(name = "district_name", nullable = false, length = 100)
    private String districtName;

    @Column(name = "ward_name", nullable = false, length = 100)
    private String wardName;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    // ── Room details ──────────────────────────────────────────────
    @Column(name = "area_size", precision = 8, scale = 2)
    private BigDecimal areaSize;

    @Column(name = "price_per_month", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerMonth;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "capacity")
    @Builder.Default
    private Integer capacity = 1;

    @Column(name = "room_type", length = 50)
    private String roomType; // single, double, shared, etc.

    @Column(name = "furnish_level", length = 50)
    private String furnishLevel;

    // ── Status ────────────────────────────────────────────────────
    @Column(name = "availability_status", length = 50)
    @Builder.Default
    private String availabilityStatus = "available";

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    // ── Timestamps ────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "display_until")
    private LocalDateTime displayUntil;

    @Column(name = "hidden_reason", columnDefinition = "TEXT")
    private String hiddenReason;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;


    // ── Relations ─────────────────────────────────────────────────
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RoomImages> images = new HashSet<>();

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RoomAmenities> amenities = new HashSet<>();
}