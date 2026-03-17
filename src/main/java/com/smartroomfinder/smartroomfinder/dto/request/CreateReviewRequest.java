package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateReviewRequest {

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5.0")
    private BigDecimal rating;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
    @Size(max = 3, message = "Maximum 3 images per review")
    private List<String> imageUrls;
}