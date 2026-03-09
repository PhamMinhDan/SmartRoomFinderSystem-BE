package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityRequest {

    @NotBlank(message = "Tên tiện ích không được để trống")
    private String amenityName;

    private String description;

    private String iconUrl;

    private String category;

    private Boolean isActive;
}