package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FavouriteResponse {
    private Long favId;
    private Long roomId;
    private boolean saved;
    private LocalDateTime createdAt;
    private RoomResponse room;
}