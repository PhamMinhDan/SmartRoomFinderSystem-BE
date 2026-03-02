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

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "City name is required")
    private String cityName;

    @NotBlank(message = "District name is required")
    private String districtName;

    @NotBlank(message = "Ward name is required")
    private String wardName;
}