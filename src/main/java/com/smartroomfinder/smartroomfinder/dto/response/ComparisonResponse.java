package com.smartroomfinder.smartroomfinder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResponse {
    private Integer totalRooms;
    private List<RoomResponse> rooms;
    private Double lowestPrice;
    private Double highestPrice;
    private Integer smallestArea;
    private Integer largestArea;
}