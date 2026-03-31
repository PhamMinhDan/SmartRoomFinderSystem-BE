package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.RoomVersion;
import com.smartroomfinder.smartroomfinder.entities.Rooms;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomVersionRepository extends JpaRepository<RoomVersion, Long> {

    Page<RoomVersion> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    boolean existsByRoomAndStatus(Rooms room, String status);

    Optional<RoomVersion> findTopByRoomAndStatusOrderByCreatedAtDesc(Rooms room, String status);

    long countByStatus(String status);
}