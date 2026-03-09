package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255)
    private String title;

    private String description;

    // ── Address ──────────────────────────────────────────────────
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String cityName;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String districtName;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String wardName;

    private BigDecimal latitude;
    private BigDecimal longitude;

    // ── Room details ──────────────────────────────────────────────
    @DecimalMin(value = "1", message = "Diện tích phải lớn hơn 0")
    private BigDecimal areaSize;

    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Giá thuê phải lớn hơn 0")
    private BigDecimal pricePerMonth;

    @DecimalMin(value = "0", message = "Tiền cọc không được âm")
    private BigDecimal depositAmount;

    private Integer capacity;
    private String roomType;
    private String furnishLevel;
    private LocalDate availableFrom;

    // ── Media ─────────────────────────────────────────────────────
    private List<String> imageUrls;

    // ── Amenities ─────────────────────────────────────────────────
    private List<Long> amenityIds;

    private List<String> customAmenities;
}