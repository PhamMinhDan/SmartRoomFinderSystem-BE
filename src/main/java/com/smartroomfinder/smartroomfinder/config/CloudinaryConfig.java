package com.smartroomfinder.smartroomfinder.config;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
@RequiredArgsConstructor
public class CloudinaryConfig {
    private final CloudinaryProperties cloudinaryProperties;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudinaryProperties.getCloudName());
        config.put("api_key", cloudinaryProperties.getApiKey());
        config.put("api_secret", cloudinaryProperties.getApiSecret());
        config.put("secure", "true");

        return new Cloudinary(config);
    }
}

