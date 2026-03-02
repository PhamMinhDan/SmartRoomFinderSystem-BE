package com.smartroomfinder.smartroomfinder.mappers;

import com.smartroomfinder.smartroomfinder.dto.response.AddressResponse;
import com.smartroomfinder.smartroomfinder.entities.Addresses;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressResponse toResponse(Addresses address) {
        if (address == null) return null;
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .streetAddress(address.getStreetAddress())
                .cityName(address.getCityName())
                .districtName(address.getDistrictName())
                .wardName(address.getWardName())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isPrimary(address.getIsPrimary())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}