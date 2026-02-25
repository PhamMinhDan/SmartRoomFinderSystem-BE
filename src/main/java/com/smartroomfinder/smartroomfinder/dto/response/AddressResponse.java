package com.smartroomfinder.smartroomfinder.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressResponse {

    @JsonProperty("address_id")
    private Integer addressId;

    @JsonProperty("street_address")
    private String streetAddress;

    @JsonProperty("city_name")
    private String cityName;

    @JsonProperty("district_name")
    private String districtName;

    @JsonProperty("ward_name")
    private String wardName;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("is_primary")
    private Boolean isPrimary;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}