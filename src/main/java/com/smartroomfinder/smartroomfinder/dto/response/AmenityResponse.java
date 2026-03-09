package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityResponse {

    private Long amenityId;

    private String amenityName;

    private String description;

    private String iconUrl;

    private String category;

    private Boolean isActive;

}