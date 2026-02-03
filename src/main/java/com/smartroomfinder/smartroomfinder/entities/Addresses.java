package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "addresses",
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_is_primary", columnList = "is_primary"),
                @Index(name = "idx_lat_long", columnList = "latitude,longitude")
        }
)
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Addresses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Integer addressId;

    // User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // Address Info
    @Column(name = "street_address", columnDefinition = "TEXT")
    private String streetAddress;

    @Column(name = "city_name", length = 100)
    private String cityName;

    @Column(name = "district_name", length = 100)
    private String districtName;

    @Column(name = "ward_name", length = 100)
    private String wardName;

    // Coordinates
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    // Primary address
    @Column(name = "is_primary")
    private Boolean isPrimary = true;

    // Audit
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}

