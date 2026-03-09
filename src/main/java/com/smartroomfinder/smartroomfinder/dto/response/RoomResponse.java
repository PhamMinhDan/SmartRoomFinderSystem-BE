package com.smartroomfinder.smartroomfinder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomResponse {

    private Long roomId;
    private String title;
    private String description;

    // Address
    private String address;
    private String cityName;
    private String districtName;
    private String wardName;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // Details
    private BigDecimal areaSize;
    private BigDecimal pricePerMonth;
    private BigDecimal depositAmount;
    private Integer capacity;
    private String roomType;
    private String furnishLevel;
    private LocalDate availableFrom;

    // Status
    private String availabilityStatus;
    private Boolean isVerified;
    private Boolean isApproved;
    private Boolean isActive;
    private Integer viewCount;
    private BigDecimal averageRating;
    private Integer totalReviews;

    // Landlord info
    private Long landlordId;
    private String landlordName;
    private String landlordAvatar;

    // Media & amenities
    private List<RoomImageResponse> images;
    private List<AmenityResponse> amenities;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
