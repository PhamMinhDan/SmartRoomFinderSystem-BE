package com.smartroomfinder.smartroomfinder.services.address;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartroomfinder.smartroomfinder.dto.request.AddressUpdateRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AddressResponse;
import com.smartroomfinder.smartroomfinder.entities.Addresses;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.mappers.AddressMapper;
import com.smartroomfinder.smartroomfinder.repositories.AddressRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import com.smartroomfinder.smartroomfinder.services.MapboxService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MapboxService mapboxService;
    @Value("${mapbox.token}")
    private String MAPBOX_TOKEN;

    @Override
    public AddressResponse getPrimaryAddress(UUID userId) {
        Users user = getUser(userId);
        Addresses address = addressRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new RuntimeException("No primary address found"));
        return addressMapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse upsertPrimaryAddress(UUID userId, AddressUpdateRequest request) {
        Users user = getUser(userId);

        String fullAddress = buildFullAddress(request);
        log.info("Geocoding address: {}", fullAddress);

        BigDecimal[] latLng = mapboxService.geocode(fullAddress);
        log.info("Geocode result → lat={}, lng={}", latLng[0], latLng[1]);

        Addresses address = addressRepository.findByUserAndIsPrimaryTrue(user)
                .orElseGet(() -> Addresses.builder()
                        .user(user)
                        .isPrimary(true)
                        .build());

        address.setStreetAddress(request.getStreetAddress());
        address.setCityName(request.getCityName());
        address.setDistrictName(request.getDistrictName());
        address.setWardName(request.getWardName());
        address.setLatitude(latLng[0]);
        address.setLongitude(latLng[1]);

        Addresses saved = addressRepository.save(address);
        user.setAddress(saved);
        userRepository.save(user);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePrimaryAddress(UUID userId) {
        Users user = getUser(userId);
        Addresses address = addressRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new RuntimeException("No primary address found"));

        user.setAddress(null);
        userRepository.save(user);
        addressRepository.delete(address);
    }


    private Users getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private String buildFullAddress(AddressUpdateRequest req) {
        return req.getStreetAddress() + ", "
                + req.getWardName() + ", "
                + req.getDistrictName() + ", "
                + req.getCityName() + ", Vietnam";
    }


}
