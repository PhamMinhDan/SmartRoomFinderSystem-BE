package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReportRequest {

    @NotBlank(message = "Lý do báo cáo không được trống")
    @Pattern(
        regexp = "FRAUD|DUPLICATE|RENTED|UNREACHABLE|WRONG_INFO|WRONG_POSTER|OTHER",
        message = "Lý do không hợp lệ"
    )
    private String reason;

    @Size(max = 500, message = "Chi tiết không được vượt quá 500 ký tự")
    private String details;

    @NotBlank(message = "Số điện thoại không được trống")
    private String reporterPhone;

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không hợp lệ")
    private String reporterEmail;
}