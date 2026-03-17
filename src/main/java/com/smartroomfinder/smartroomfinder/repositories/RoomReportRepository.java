package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.RoomReports;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoomReportRepository extends JpaRepository<RoomReports, Long> {

    // Admin: lấy tất cả báo cáo, filter theo status
    Page<RoomReports> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // Admin: tất cả báo cáo không filter
    Page<RoomReports> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Kiểm tra user đã báo cáo phòng này chưa (tránh spam)
    boolean existsByRoom_RoomIdAndReporter_UserId(Long roomId, UUID reporterId);

    // Đếm báo cáo pending theo roomId (để hiển thị badge)
    long countByRoom_RoomIdAndStatus(Long roomId, String status);

    // Lấy báo cáo theo roomId cho admin xem chi tiết
    @Query("SELECT r FROM RoomReports r WHERE r.room.roomId = :roomId ORDER BY r.createdAt DESC")
    Page<RoomReports> findByRoomId(@Param("roomId") Long roomId, Pageable pageable);
}