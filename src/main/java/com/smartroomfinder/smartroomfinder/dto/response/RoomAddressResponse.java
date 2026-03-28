package com.smartroomfinder.smartroomfinder.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAddressResponse {

    @JsonProperty("room_address_id")
    private Long roomAddressId;

    @JsonProperty("street_address")
    private String streetAddress;

    @JsonProperty("city_name")
    private String cityName;

    @JsonProperty("district_name")
    private String districtName;

    @JsonProperty("ward_name")
    private String wardName;

    @JsonProperty("latitude")
    private BigDecimal latitude;

    @JsonProperty("longitude")
    private BigDecimal longitude;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}