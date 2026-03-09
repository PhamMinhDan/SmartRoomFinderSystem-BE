package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.request.AmenityRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AmenityResponse;
import com.smartroomfinder.smartroomfinder.entities.Amenities;
import com.smartroomfinder.smartroomfinder.mappers.AmenityMapper;
import com.smartroomfinder.smartroomfinder.repositories.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final AmenityMapper amenityMapper;

    public List<AmenityResponse> getAll() {
        return amenityRepository.findAll()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    public List<AmenityResponse> getActive() {
        return amenityRepository.findByIsActiveTrue()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    public AmenityResponse getById(Long id) {
        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        return amenityMapper.toResponse(amenity);
    }

    public AmenityResponse create(AmenityRequest request) {

        if (amenityRepository.existsByAmenityNameIgnoreCase(request.getAmenityName())) {
            throw new RuntimeException("Amenity already exists");
        }

        Amenities entity = amenityMapper.toEntity(request);

        Amenities saved = amenityRepository.save(entity);

        return amenityMapper.toResponse(saved);
    }

    public AmenityResponse update(Long id, AmenityRequest request) {

        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        amenityMapper.updateEntity(amenity, request);

        Amenities updated = amenityRepository.save(amenity);

        return amenityMapper.toResponse(updated);
    }

    public void delete(Long id) {

        Amenities amenity = amenityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Amenity not found"));

        amenityRepository.delete(amenity);
    }
}