package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long reviewId;
    private Long roomId;
    private String userId;
    private String userName;
    private String userAvatar;
    private BigDecimal rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}