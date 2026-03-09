package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.AmenityRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AmenityResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.services.AmenityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    @GetMapping
    public ApiResponse<List<AmenityResponse>> getActive() {

        return ApiResponse.success(
                amenityService.getActive(),
                "OK"
        );
    }

    @GetMapping("/all")
    public ApiResponse<List<AmenityResponse>> getAll() {

        return ApiResponse.success(
                amenityService.getAll(),
                "OK"
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<AmenityResponse> getById(@PathVariable Long id) {

        return ApiResponse.success(
                amenityService.getById(id),
                "OK"
        );
    }

    @PostMapping
    public ApiResponse<AmenityResponse> create(
            @Valid @RequestBody AmenityRequest request
    ) {

        return ApiResponse.success(
                amenityService.create(request),
                "Amenity created"
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<AmenityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AmenityRequest request
    ) {

        return ApiResponse.success(
                amenityService.update(id, request),
                "Amenity updated"
        );
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {

        amenityService.delete(id);

        return ApiResponse.success(
                null,
                "Amenity deleted"
        );
    }
}