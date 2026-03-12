package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.FavouriteResponse;
import com.smartroomfinder.smartroomfinder.services.FavouriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/favourites")
@RequiredArgsConstructor
public class FavouriteController {

    private final FavouriteService favouriteService;

    /**
     * POST /api/favourites/{roomId}/toggle
     * Toggle save/unsave. Returns { saved: true/false }.
     * Requires authentication.
     */
    @PostMapping("/{roomId}/toggle")
    public ResponseEntity<ApiResponse<FavouriteResponse>> toggle(@PathVariable Long roomId) {
        try {
            UUID userId = extractUserId();
            FavouriteResponse result = favouriteService.toggle(roomId, userId);
            String msg = result.isSaved() ? "Đã lưu tin" : "Đã bỏ lưu tin";
            return ResponseEntity.ok(ApiResponse.success(result, msg));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    /**
     * GET /api/favourites?page=0&size=12
     * Paginated list of saved rooms for the current user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FavouriteResponse>>> getMyFavourites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        try {
            UUID userId = extractUserId();
            return ResponseEntity.ok(
                    ApiResponse.success(favouriteService.getMyFavourites(userId, page, size), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        }
    }

    /**
     * GET /api/favourites/ids
     * Returns Set<Long> of all roomIds saved by the current user.
     * Used by search/home page to pre-highlight heart buttons.
     */
    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<Set<Long>>> getSavedIds() {
        try {
            UUID userId = extractUserId();
            return ResponseEntity.ok(ApiResponse.success(favouriteService.getSavedRoomIds(userId), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        }
    }

    /**
     * GET /api/favourites/{roomId}/status
     * Check if a single room is saved by the current user.
     */
    @GetMapping("/{roomId}/status")
    public ResponseEntity<ApiResponse<Boolean>> getStatus(@PathVariable Long roomId) {
        try {
            UUID userId = extractUserId();
            return ResponseEntity.ok(
                    ApiResponse.success(favouriteService.isSaved(roomId, userId), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(String.valueOf(false), "UNAUTHORIZED"));
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