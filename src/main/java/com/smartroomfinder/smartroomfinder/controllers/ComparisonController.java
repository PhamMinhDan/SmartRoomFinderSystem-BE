package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ComparisonResponse;
import com.smartroomfinder.smartroomfinder.services.ComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comparison")
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonService comparisonService;

    /**
     * So sánh nhiều phòng
     * GET /api/comparison?roomIds=1,2,3,4
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ComparisonResponse>> compareRooms(
            @RequestParam List<Long> roomIds) {
        try {
            ComparisonResponse response = comparisonService.compareRooms(roomIds);
            return ResponseEntity.ok(ApiResponse.success(response, "So sánh thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "BAD_REQUEST"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        }
    }
}