package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Reviews;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import com.smartroomfinder.smartroomfinder.entities.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Reviews, Long> {

    Page<Reviews> findByRoomAndIsActiveTrueOrderByCreatedAtDesc(Rooms room, Pageable pageable);

    Optional<Reviews> findByRoomAndUser(Rooms room, Users user);

    boolean existsByRoomAndUser(Rooms room, Users user);

    @Query("SELECT r FROM Reviews r JOIN FETCH r.user WHERE r.room.roomId = :roomId AND r.isActive = true ORDER BY r.createdAt DESC")
    Page<Reviews> findByRoomIdWithUser(@Param("roomId") Long roomId, Pageable pageable);


    @Query("SELECT AVG(r.rating) FROM Reviews r WHERE r.room.roomId = :roomId AND r.isActive = true")
    Optional<BigDecimal> calcAverageRatingByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(r) FROM Reviews r WHERE r.room.roomId = :roomId AND r.isActive = true")
    int countActiveByRoomId(@Param("roomId") Long roomId);
}