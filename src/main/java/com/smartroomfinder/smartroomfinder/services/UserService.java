package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.response.UserResponse;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.mappers.UserMapper;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import com.smartroomfinder.smartroomfinder.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired token");
                throw new RuntimeException("Invalid or expired token");
            }

            String userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null || userId.isEmpty()) {
                log.warn("Could not extract userId from token");
                throw new RuntimeException("Could not extract userId from token");
            }

            UUID parsedUserId = UUID.fromString(userId);
            Optional<Users> userOpt = userRepository.findById(parsedUserId);

            if (userOpt.isEmpty()) {
                log.warn("User not found - UserId: {}", userId);
                throw new RuntimeException("User not found");
            }

            log.info("User found - UserId: {}", userId);
            return userMapper.toResponse(userOpt.get());

        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format: {}", e.getMessage());
            throw new RuntimeException("Invalid userId format");
        }
    }

}