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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        BigDecimal[] latLng = geocode(fullAddress);
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


    private BigDecimal[] geocode(String address) {
        try {
            // Encode query param thủ công — tránh lỗi encode tiếng Việt
            String encodedQuery = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search"
                    + "?q=" + encodedQuery
                    + "&format=json"
                    + "&limit=1"
                    + "&countrycodes=vn"
                    + "&accept-language=vi";

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "SmartRoomFinder/1.0 (contact@smartroomfinder.com)");
            headers.set(HttpHeaders.ACCEPT, "application/json");
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            log.debug("Nominatim response: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                BigDecimal lat = new BigDecimal(first.get("lat").asText());
                BigDecimal lng = new BigDecimal(first.get("lon").asText());
                log.info("Geocoded '{}' → lat={}, lng={}", address, lat, lng);
                return new BigDecimal[]{lat, lng};
            }

            log.warn("⚠Nominatim returned 0 results for: '{}'", address);

            return geocodeFallback(address);

        } catch (Exception e) {
            log.error("Geocoding error for '{}': {}", address, e.getMessage());
        }

        // Fallback cuối: Hà Nội center
        log.warn("Using default Hanoi coordinates as final fallback");
        return new BigDecimal[]{new BigDecimal("21.0285"), new BigDecimal("105.8542")};
    }

    private BigDecimal[] geocodeFallback(String originalAddress) {
        try {

            String[] parts = originalAddress.split(", ");
            if (parts.length >= 3) {
                String simpleAddress = parts[parts.length - 3]
                        + ", " + parts[parts.length - 2]
                        + ", " + parts[parts.length - 1];

                log.info("Retrying geocode with simplified address: '{}'", simpleAddress);

                String encodedQuery = URLEncoder.encode(simpleAddress, StandardCharsets.UTF_8);
                String url = "https://nominatim.openstreetmap.org/search"
                        + "?q=" + encodedQuery
                        + "&format=json&limit=1&countrycodes=vn";

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.USER_AGENT, "SmartRoomFinder/1.0");

                ResponseEntity<String> response = restTemplate.exchange(
                        URI.create(url),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );

                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.isArray() && root.size() > 0) {
                    JsonNode first = root.get(0);
                    BigDecimal lat = new BigDecimal(first.get("lat").asText());
                    BigDecimal lng = new BigDecimal(first.get("lon").asText());
                    log.info("Fallback geocoded '{}' → lat={}, lng={}", simpleAddress, lat, lng);
                    return new BigDecimal[]{lat, lng};
                }
            }
        } catch (Exception e) {
            log.error("Fallback geocoding error: {}", e.getMessage());
        }
        return new BigDecimal[]{new BigDecimal("21.0285"), new BigDecimal("105.8542")};
    }
}