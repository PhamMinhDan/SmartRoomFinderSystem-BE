package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResolveReportRequest {

    // RESOLVED | DISMISSED
    @NotBlank(message = "Trạng thái xử lý không được trống")
    @Pattern(regexp = "RESOLVED|DISMISSED", message = "Trạng thái không hợp lệ")
    private String status;

    private String adminNote;
}