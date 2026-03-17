package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.response.ComparisonResponse;
import com.smartroomfinder.smartroomfinder.dto.response.RoomResponse;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import com.smartroomfinder.smartroomfinder.mappers.RoomMapper;
import com.smartroomfinder.smartroomfinder.repositories.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public ComparisonResponse compareRooms(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách phòng không được trống");
        }
        if (roomIds.size() > 4) {
            throw new IllegalArgumentException("Tối đa chỉ so sánh 4 phòng");
        }

        List<Rooms> rooms = roomRepository.findAllById(roomIds);
        if (rooms.isEmpty()) {
            throw new RuntimeException("Không tìm thấy phòng nào");
        }

        List<RoomResponse> roomResponses = rooms.stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());

        return ComparisonResponse.builder()
                .totalRooms(roomResponses.size())
                .rooms(roomResponses)
                .lowestPrice(getLowestPrice(roomResponses))
                .highestPrice(getHighestPrice(roomResponses))
                .smallestArea(getSmallestArea(roomResponses))
                .largestArea(getLargestArea(roomResponses))
                .build();
    }

    private Double getLowestPrice(List<RoomResponse> rooms) {
        return rooms.stream()
                .map(RoomResponse::getPricePerMonth)
                .filter(p -> p != null)
                .min(Comparator.naturalOrder())
                .map(BigDecimal::doubleValue)
                .orElse(0.0);
    }

    private Double getHighestPrice(List<RoomResponse> rooms) {
        return rooms.stream()
                .map(RoomResponse::getPricePerMonth)
                .filter(p -> p != null)
                .max(Comparator.naturalOrder())
                .map(BigDecimal::doubleValue)
                .orElse(0.0);
    }

    private Integer getSmallestArea(List<RoomResponse> rooms) {
        return rooms.stream()
                .map(RoomResponse::getAreaSize)
                .filter(a -> a != null)
                // Dùng BigDecimal.compareTo để so sánh đúng, tránh lỗi cast
                .min(Comparator.naturalOrder())
                .map(BigDecimal::intValue)
                .orElse(0);
    }

    private Integer getLargestArea(List<RoomResponse> rooms) {
        return rooms.stream()
                .map(RoomResponse::getAreaSize)
                .filter(a -> a != null)
                .max(Comparator.naturalOrder())
                .map(BigDecimal::intValue)
                .orElse(0);
    }
}