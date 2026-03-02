package com.smartroomfinder.smartroomfinder.controllers;

import com.smartroomfinder.smartroomfinder.dto.request.AddressUpdateRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AddressResponse;
import com.smartroomfinder.smartroomfinder.dto.response.ApiResponse;
import com.smartroomfinder.smartroomfinder.services.address.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/me/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<AddressResponse>> getPrimaryAddress() {
        try {
            UUID userId = extractUserId();
            AddressResponse response = addressService.getPrimaryAddress(userId);
            return ResponseEntity.ok(ApiResponse.success(response, "Address retrieved successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (RuntimeException e) {
            log.warn("Get address error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage(), "NOT_FOUND"));
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Hệ thống gặp lỗi", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AddressResponse>> upsertPrimaryAddress(
            @Valid @RequestBody AddressUpdateRequest request) {
        try {
            UUID userId = extractUserId();
            AddressResponse response = addressService.upsertPrimaryAddress(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Address updated successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            log.error("Upsert address error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Hệ thống gặp lỗi", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deletePrimaryAddress() {
        try {
            UUID userId = extractUserId();
            addressService.deletePrimaryAddress(userId);
            return ResponseEntity.ok(ApiResponse.success(null, "Address deleted successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            log.error("Delete address error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Hệ thống gặp lỗi", e.getMessage()));
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