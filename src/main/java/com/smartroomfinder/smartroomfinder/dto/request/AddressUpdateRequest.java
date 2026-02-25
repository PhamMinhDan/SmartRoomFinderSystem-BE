package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressUpdateRequest {

    @Size(max = 500, message = "Street address không được vượt quá 500 ký tự")
    private String streetAddress;

    @Size(max = 100, message = "City name không được vượt quá 100 ký tự")
    private String cityName;

    @Size(max = 100, message = "District name không được vượt quá 100 ký tự")
    private String districtName;

    @Size(max = 100, message = "Ward name không được vượt quá 100 ký tự")
    private String wardName;

    // Coordinates (latitude, longitude)
    @DecimalMin(value = "-90.0", message = "Latitude phải >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude phải <= 90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude phải >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude phải <= 180.0")
    private Double longitude;

    private Boolean isPrimary;
}