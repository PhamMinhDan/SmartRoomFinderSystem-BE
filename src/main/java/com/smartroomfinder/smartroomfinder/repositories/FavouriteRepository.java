package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Favourites;
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
import java.util.Set;
import java.util.UUID;

@Repository
public interface FavouriteRepository extends JpaRepository<Favourites, Long> {

    Optional<Favourites> findByUserAndRoom(Users user, Rooms room);

    boolean existsByUserAndRoom(Users user, Rooms room);

    void deleteByUserAndRoom(Users user, Rooms room);

    @Query("SELECT f FROM Favourites f JOIN FETCH f.room r WHERE f.user.userId = :userId ORDER BY f.createdAt DESC")
    Page<Favourites> findByUserIdWithRoom(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT f.room.roomId FROM Favourites f WHERE f.user.userId = :userId")
    Set<Long> findRoomIdsByUserId(@Param("userId") UUID userId);
}