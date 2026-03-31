package com.smartroomfinder.smartroomfinder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomSnapshot {

    private String title;
    private String description;
    private BigDecimal pricePerMonth;
    private BigDecimal depositAmount;
    private BigDecimal areaSize;
    private Integer capacity;
    private String roomType;
    private String furnishLevel;
    private String availableFrom;

    // ── Address ───────────────────────────────
    private String streetAddress;
    private String wardName;
    private String districtName;
    private String cityName;

    // ── Media & Amenities ─────────────────────
    private List<String> mediaUrls;
    private List<Long> amenityIds;
    private List<String> amenityNames;
}