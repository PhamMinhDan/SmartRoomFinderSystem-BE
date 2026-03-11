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

            String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
                    + address
                    + ".json?access_token=" + MAPBOX_TOKEN
                    + "&limit=1"
                    + "&country=vn"
                    + "&language=vi";

            log.info("Mapbox URL: {}", url);

            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode features = root.get("features");

            if (features != null && features.size() > 0) {

                JsonNode center = features.get(0).get("center");

                BigDecimal lng = new BigDecimal(center.get(0).asText());
                BigDecimal lat = new BigDecimal(center.get(1).asText());

                log.info("Mapbox geocode '{}' → lat={}, lng={}", address, lat, lng);

                return new BigDecimal[]{lat, lng};
            }

        } catch (Exception e) {

            log.error("Mapbox geocode error: {}", e.getMessage());
        }

        return new BigDecimal[]{
                new BigDecimal("21.0285"),
                new BigDecimal("105.8542")
        };
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
                String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
                        + encodedQuery
                        + ".json?access_token=" + MAPBOX_TOKEN
                        + "&autocomplete=false"
                        + "&limit=1"
                        + "&country=vn"
                        + "&language=vi";

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