package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoomRatingStats {
    private Long roomId;
    private BigDecimal averageRating;
    private int totalReviews;
}