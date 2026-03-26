package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.CreateRoomRequest;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomResponse;
import com.smartroomfinder.smartroomfinder.services.IdentityVerificationService;
import com.smartroomfinder.smartroomfinder.services.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final IdentityVerificationService identityVerificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @Valid @RequestBody CreateRoomRequest request) {
        try {
            UUID userId = extractUserId();
            RoomResponse room = roomService.createRoom(request, userId);
            identityVerificationService.promoteToLandlordIfVerified(userId);
            return ResponseEntity.ok(ApiResponse.success(room, "Đăng phòng thành công"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(roomService.getRoomById(id), "OK"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> getMyRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UUID userId = extractUserId();
            return ResponseEntity.ok(ApiResponse.success(
                    roomService.getMyRooms(userId, page, size), "OK"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> searchRooms(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) BigDecimal areaMin,
            @RequestParam(required = false) BigDecimal areaMax,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        roomService.searchRooms(
                                city,
                                district,
                                roomType,
                                priceMin,
                                priceMax,
                                areaMin,
                                areaMax,
                                minRating,
                                amenities,
                                page,
                                size,
                                sort
                        ),
                        "OK"
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody CreateRoomRequest request) {
        try {
            UUID userId = extractUserId();
            return ResponseEntity.ok(ApiResponse.success(
                    roomService.updateRoom(id, request, userId), "Cập nhật phòng thành công"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        try {
            UUID userId = extractUserId();
            roomService.deleteRoom(id, userId);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa phòng thành công"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        }
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        try {
            roomService.incrementViewCount(id);
        } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ApiResponse<RoomResponse>> toggleActive(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            UUID userId = extractUserId();

            boolean isActive = (Boolean) body.get("isActive");
            String reason = (String) body.getOrDefault("reason", null);

            return ResponseEntity.ok(
                    ApiResponse.success(
                            roomService.setRoomActive(id, userId, isActive, reason),
                            "Cập nhật trạng thái thành công"
                    )
            );
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

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> getFeaturedRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {

        return ResponseEntity.ok(
                ApiResponse.success(roomService.getFeaturedRooms(page, size), "OK")
        );
    }

    @PatchMapping("/{id}/extend")
    public ResponseEntity<ApiResponse<RoomResponse>> extendRoom(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {

        UUID userId = extractUserId();
        int days = body.get("days");

        return ResponseEntity.ok(
                ApiResponse.success(
                        roomService.extendRoom(id, userId, days),
                        "Gia hạn thành công"
                )
        );
    }
}