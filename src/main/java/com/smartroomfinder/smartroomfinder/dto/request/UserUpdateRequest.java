package com.smartroomfinder.smartroomfinder.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequest {

    @NotBlank(message = "Full name không được để trống")
    @Size(min = 2, max = 255, message = "Full name phải từ 2 đến 255 ký tự")
    private String fullName;

    @Pattern(
            regexp = "^0[0-9]{9}$",
            message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0"
    )
    private String phoneNumber;

    @Size(max = 500, message = "Bio không được vượt quá 500 ký tự")
    private String bio;

    @Size(max = 500, message = "Avatar URL không được vượt quá 500 ký tự")
    private String avatarUrl;

    // Address info
    @Valid
    private AddressUpdateRequest address;
}