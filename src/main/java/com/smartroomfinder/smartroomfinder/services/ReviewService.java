package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.request.CreateReviewRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ReviewResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomRatingStats;
import com.smartroomfinder.smartroomfinder.entities.Reviews;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.mappers.ReviewMapper;
import com.smartroomfinder.smartroomfinder.repositories.ReviewRepository;
import com.smartroomfinder.smartroomfinder.repositories.RoomRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    // ── Create ────────────────────────────────────────────────────
    @Transactional
    public ReviewResponse createReview(Long roomId, CreateReviewRequest req, UUID userId) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Landlord cannot review their own room
        if (room.getLandlord().getUserId().equals(userId)) {
            throw new IllegalStateException("Chủ phòng không thể đánh giá phòng của mình");
        }

        // Validate image count (1–3)
        List<String> imageUrls = req.getImageUrls();
        if (imageUrls != null && imageUrls.size() > 3) {
            throw new IllegalArgumentException("Tối đa 3 ảnh cho mỗi đánh giá");
        }

        Reviews review = Reviews.builder()
                .room(room)
                .user(user)
                .rating(req.getRating().setScale(1, RoundingMode.HALF_UP))
                .comment(req.getComment())
                .build();

        // Persist image URLs
        if (imageUrls != null && !imageUrls.isEmpty()) {
            review.setImageUrlList(imageUrls);
        }

        Reviews saved = reviewRepository.save(review);

        // Recalculate and persist average rating on the Room entity
        recalcRoomRating(roomId);

        log.info("Review created - roomId: {}, userId: {}, rating: {}, images: {}",
                roomId, userId, req.getRating(),
                imageUrls != null ? imageUrls.size() : 0);
        return reviewMapper.toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByRoom(Long roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByRoomIdWithUser(roomId, pageable)
                .map(reviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RoomRatingStats getRatingStats(Long roomId) {
        BigDecimal avg = reviewRepository.calcAverageRatingByRoomId(roomId)
                .map(v -> v.setScale(2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
        int total = reviewRepository.countActiveByRoomId(roomId);
        return RoomRatingStats.builder()
                .roomId(roomId)
                .averageRating(avg)
                .totalReviews(total)
                .build();
    }

    // ── Delete ────────────────────────────────────────────────────
    @Transactional
    public void deleteReview(Long reviewId, UUID userId) {
        Reviews review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này");
        }

        Long roomId = review.getRoom().getRoomId();
        review.setIsActive(false);
        reviewRepository.save(review);

        recalcRoomRating(roomId);
    }

    // ── Helper: recalculate and persist avg rating on Rooms ───────
    @Transactional
    public void recalcRoomRating(Long roomId) {
        BigDecimal avg = reviewRepository.calcAverageRatingByRoomId(roomId)
                .map(v -> v.setScale(2, RoundingMode.HALF_UP))
                .orElse(null);
        int total = reviewRepository.countActiveByRoomId(roomId);

        roomRepository.findById(roomId).ifPresent(room -> {
            room.setAverageRating(avg);
            room.setTotalReviews(total);
            roomRepository.save(room);
        });
    }

    public Page<ReviewResponse> getReviewsFilter(
            Long roomId, int page, int size, Integer star, String sort) {

        Sort sorting = switch (sort) {
            case "high" -> Sort.by("rating").descending();
            case "low" -> Sort.by("rating").ascending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<Reviews> pageData;

        if (star != null) {
            pageData = reviewRepository.findByRoom_RoomIdAndRatingBetween(
                    roomId,
                    BigDecimal.valueOf(star),
                    BigDecimal.valueOf(star + 1),
                    pageable
            );
        } else {
            pageData = reviewRepository.findByRoom_RoomId(roomId, pageable);
        }

        return pageData.map(reviewMapper::toResponse);
    }

    public Map<Integer, Long> getStarCounts(Long roomId) {

        List<Object[]> results = reviewRepository.countReviewsByStar(roomId);

        Map<Integer, Long> map = new HashMap<>();

        for (Object[] row : results) {
            int star = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            map.put(star, count);
        }

        // đảm bảo đủ 1→5
        for (int i = 1; i <= 5; i++) {
            map.putIfAbsent(i, 0L);
        }

        return map;
    }
}