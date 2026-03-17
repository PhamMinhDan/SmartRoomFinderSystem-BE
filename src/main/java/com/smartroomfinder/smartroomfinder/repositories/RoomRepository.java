package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Rooms;
import com.smartroomfinder.smartroomfinder.entities.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Rooms, Long> {

    Page<Rooms> findByLandlordAndIsActiveTrue(Users landlord, Pageable pageable);

    @Query("""
SELECT DISTINCT r FROM Rooms r
LEFT JOIN FETCH r.images
LEFT JOIN FETCH r.amenities ra
LEFT JOIN FETCH ra.amenity
WHERE r.roomId = :roomId AND r.isActive = true
""")
    Optional<Rooms> findByIdWithDetails(@Param("roomId") Long roomId);

    @Query("""
    SELECT r FROM Rooms r
    WHERE r.isApproved = true
    AND r.isActive = true
    AND (r.displayUntil IS NULL OR r.displayUntil > CURRENT_TIMESTAMP)
    AND (:city IS NULL OR r.cityName = :city)
    AND (:district IS NULL OR r.districtName = :district)
    ORDER BY r.createdAt DESC
""")
    Page<Rooms> findApprovedRooms(
            @Param("city") String city,
            @Param("district") String district,
            Pageable pageable
    );

    // ── Admin queries ─────────────────────────────────────────────
    Page<Rooms> findByIsApprovedFalseAndIsActiveTrue(Pageable pageable);

    Page<Rooms> findByIsApprovedAndIsActiveTrue(Boolean isApproved, Pageable pageable);

    Page<Rooms> findByIsActiveTrue(Pageable pageable);

    long countByIsApprovedFalseAndIsActiveTrue();

    long countByIsApprovedTrueAndIsActiveTrue();

    List<Rooms> findByLandlord(Users landlord);

    long countByLandlord(Users landlord);
}