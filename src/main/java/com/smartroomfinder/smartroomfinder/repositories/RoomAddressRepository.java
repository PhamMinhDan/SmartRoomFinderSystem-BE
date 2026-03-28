package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.RoomAddresses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomAddressRepository extends JpaRepository<RoomAddresses, Long> {

    Optional<RoomAddresses> findByRoom_RoomId(Long roomId);

    void deleteByRoom_RoomId(Long roomId);
}