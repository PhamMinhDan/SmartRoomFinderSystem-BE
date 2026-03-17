package com.smartroomfinder.smartroomfinder.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_review_room_id", columnList = "room_id"),
                @Index(name = "idx_review_user_id", columnList = "user_id"),
                @Index(name = "idx_review_created_at", columnList = "created_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Reviews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Rooms room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "rating", nullable = false, precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Helpers ────────────────────────────────────────────────────
    public List<String> getImageUrlList() {
        if (imageUrls == null || imageUrls.isBlank()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String url : imageUrls.split(",")) {
            String trimmed = url.trim();
            if (!trimmed.isEmpty()) list.add(trimmed);
        }
        return list;
    }

    public void setImageUrlList(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            this.imageUrls = null;
        } else {
            this.imageUrls = String.join(",", urls);
        }
    }
}