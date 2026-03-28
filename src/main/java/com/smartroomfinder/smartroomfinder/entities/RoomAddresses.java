package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "room_addresses",
        indexes = {
                @Index(name = "idx_room_address_room_id", columnList = "room_id"),
                @Index(name = "idx_room_address_city",    columnList = "city_name"),
                @Index(name = "idx_room_address_district", columnList = "district_name"),
                @Index(name = "idx_room_address_lat_lng", columnList = "latitude,longitude")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoomAddresses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_address_id")
    private Long roomAddressId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Rooms room;

    @Column(name = "street_address", nullable = false, columnDefinition = "TEXT")
    private String streetAddress;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}