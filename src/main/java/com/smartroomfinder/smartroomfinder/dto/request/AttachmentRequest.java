package com.smartroomfinder.smartroomfinder.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentRequest {

    private String fileUrl;

    private String fileType;

    private Integer sortOrder;
}
