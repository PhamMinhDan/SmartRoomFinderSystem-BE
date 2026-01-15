package com.smartroomfinder.smartroomfinder.securities;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class Security {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())  // API ⇒ disable CSRF
                .cors(Customizer.withDefaults()) // dùng CorsConfig bên bạn
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/upload/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}

