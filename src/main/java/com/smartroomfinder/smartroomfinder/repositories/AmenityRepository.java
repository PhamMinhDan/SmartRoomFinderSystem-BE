package com.smartroomfinder.smartroomfinder.repositories;

import com.smartroomfinder.smartroomfinder.entities.Amenities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenities, Long> {
    List<Amenities> findByIsActiveTrue();
    List<Amenities> findByAmenityIdIn(List<Long> ids);
    boolean existsByAmenityNameIgnoreCase(String amenityName);

    Optional<Amenities> findByAmenityNameIgnoreCase(String amenityName);
}