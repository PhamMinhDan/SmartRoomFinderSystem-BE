package com.smartroomfinder.smartroomfinder.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class MapboxService {

    @Value("${mapbox.token}")
    private String MAPBOX_TOKEN;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BigDecimal[] geocode(String address) {

        try {

            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);

            String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
                    + address
                    + ".json?access_token=" + MAPBOX_TOKEN
                    +"&autocomplete=false"
                    + "&limit=1"
                    + "&country=vn"
                    + "&language=vi";

            ResponseEntity<String> response =
                    restTemplate.getForEntity(url, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode features = root.get("features");

            if (features != null && features.size() > 0) {

                JsonNode center = features.get(0).get("center");

                BigDecimal lng = new BigDecimal(center.get(0).asText());
                BigDecimal lat = new BigDecimal(center.get(1).asText());

                return new BigDecimal[]{lat, lng};
            }

        } catch (Exception e) {
            log.error("Mapbox geocode error {}", e.getMessage());
        }

        return new BigDecimal[]{
                new BigDecimal("21.0285"),
                new BigDecimal("105.8542")
        };
    }
}
