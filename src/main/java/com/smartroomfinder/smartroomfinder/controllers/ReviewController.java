package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.CreateReviewRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ReviewResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomRatingStats;
import com.smartroomfinder.smartroomfinder.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/rooms/{roomId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateReviewRequest request) {
        try {
            UUID userId = extractUserId();
            ReviewResponse review = reviewService.createReview(roomId, request, userId);
            return ResponseEntity.ok(ApiResponse.success(review, "Đánh giá thành công"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) Integer star,
            @RequestParam(defaultValue = "latest") String sort) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        reviewService.getReviewsFilter(roomId, page, size, star, sort),
                        "OK"
                )
        );
    }


    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RoomRatingStats>> getStats(@PathVariable Long roomId) {
        return ResponseEntity.ok(
                ApiResponse.success(reviewService.getRatingStats(roomId), "OK"));
    }

    @GetMapping("/stats-count")
    public ResponseEntity<ApiResponse<Map<Integer, Long>>> getReviewStats(
            @PathVariable Long roomId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        reviewService.getStarCounts(roomId),
                        "OK"
                )
        );
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long roomId,
            @PathVariable Long reviewId) {
        try {
            UUID userId = extractUserId();
            reviewService.deleteReview(reviewId, userId);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa đánh giá thành công"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage(), "FORBIDDEN"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new IllegalStateException("Unauthorized: no authentication found");
        }
        return UUID.fromString(auth.getCredentials().toString());
    }
}