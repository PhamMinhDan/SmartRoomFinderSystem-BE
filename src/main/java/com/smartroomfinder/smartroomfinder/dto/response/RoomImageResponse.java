package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomImageResponse {
    private Long imageId;
    private String imageUrl;
    private Integer imageOrder;
    private Boolean isPrimary;
}
