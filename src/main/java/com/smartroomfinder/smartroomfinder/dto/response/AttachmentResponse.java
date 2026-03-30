package com.smartroomfinder.smartroomfinder.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private Long attachmentId;

    private String fileUrl;

    private String fileType;

    private Integer sortOrder;

    private LocalDate createdAt;
}
