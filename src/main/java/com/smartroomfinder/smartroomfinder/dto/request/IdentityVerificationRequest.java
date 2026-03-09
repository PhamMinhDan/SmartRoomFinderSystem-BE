package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityVerificationRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @NotBlank(message = "Loại giấy tờ không được để trống")
    private String documentType; // CCCD, PASSPORT, DRIVER_LICENSE

    @NotBlank(message = "Ảnh mặt trước không được để trống")
    private String frontImageUrl;

    @NotBlank(message = "Ảnh mặt sau không được để trống")
    private String backImageUrl;

    private String selfieImageUrl;
}