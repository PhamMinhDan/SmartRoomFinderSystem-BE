package com.smartroomfinder.smartroomfinder.services.address;

import com.smartroomfinder.smartroomfinder.dto.request.AddressUpdateRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AddressResponse;

import java.util.UUID;

public interface AddressService {
    AddressResponse getPrimaryAddress(UUID userId);
    AddressResponse upsertPrimaryAddress(UUID userId, AddressUpdateRequest request);
    void deletePrimaryAddress(UUID userId);
}
