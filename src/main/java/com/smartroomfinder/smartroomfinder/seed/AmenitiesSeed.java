package com.smartroomfinder.smartroomfinder.seed;

import com.smartroomfinder.smartroomfinder.entities.Amenities;
import com.smartroomfinder.smartroomfinder.repositories.AmenityRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AmenitiesSeed {

    private final AmenityRepository amenityRepository;

    @PostConstruct
    public void init(){
        addAmenitiesIfNotExists();
    }

    private void addAmenitiesIfNotExists() {

        List<Amenities> amenities = List.of(

                Amenities.builder()
                        .amenityName("Wi-Fi")
                        .description("Internet tốc độ cao")
                        .category("internet")
                        .iconUrl(null)
                        .build(),

                Amenities.builder()
                        .amenityName("Điều hòa")
                        .description("Máy điều hòa không khí")
                        .category("appliance")
                        .iconUrl(null)
                        .build(),

                Amenities.builder()
                        .amenityName("Bếp riêng")
                        .description("Có khu vực bếp riêng")
                        .category("kitchen")
                        .iconUrl(null)
                        .build(),

                Amenities.builder()
                        .amenityName("Gác lửng")
                        .description("Phòng có gác lửng")
                        .category("structure")
                        .iconUrl(null)
                        .build(),

                Amenities.builder()
                        .amenityName("Máy giặt")
                        .description("Có máy giặt")
                        .category("appliance")
                        .iconUrl(null)
                        .build(),

                Amenities.builder()
                        .amenityName("Chỗ gửi xe")
                        .description("Có bãi gửi xe")
                        .category("parking")
                        .iconUrl(null)
                        .build(),

                Amenities.builder()
                        .amenityName("Ban công")
                        .description("Phòng có ban công")
                        .category("structure")
                        .iconUrl(null)
                        .build(),

                Amenities.builder()
                        .amenityName("Camera an ninh")
                        .description("Có camera giám sát")
                        .category("security")
                        .iconUrl(null)
                        .build(),

                Amenities.builder()
                        .amenityName("Thang máy")
                        .description("Có thang máy")
                        .category("building")
                        .iconUrl(null)
                        .build()
        );

        for (Amenities amenity : amenities) {
            boolean exists = amenityRepository
                    .findAll()
                    .stream()
                    .anyMatch(a -> a.getAmenityName().equalsIgnoreCase(amenity.getAmenityName()));

            if (!exists) {
                amenityRepository.save(amenity);
            }
        }
    }
}