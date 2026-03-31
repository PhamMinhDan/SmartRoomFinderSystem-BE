package com.smartroomfinder.smartroomfinder.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomVersionResponse {

    private Long versionId;
    private Long roomId;
    private String status;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    // ── Landlord ───────────────────────────────
    private String landlordId;
    private String landlordName;
    private String landlordEmail;
    private String landlordPhone;
    private String landlordAvatar;

    // ── So sánh dữ liệu ───────────────────────
    private RoomSnapshot oldData;
    private RoomSnapshot newData;
}