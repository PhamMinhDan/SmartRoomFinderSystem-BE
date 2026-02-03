package com.smartroomfinder.smartroomfinder.services;

import com.smartroomfinder.smartroomfinder.dto.request.FacebookLoginRequest;
import com.smartroomfinder.smartroomfinder.dto.response.AuthFacebookResponse;
import com.smartroomfinder.smartroomfinder.entities.Roles;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.exceptions.BadRequestException;
import com.smartroomfinder.smartroomfinder.exceptions.ResourceNotFoundException;
import com.smartroomfinder.smartroomfinder.mappers.UserMapper;
import com.smartroomfinder.smartroomfinder.repositories.RoleRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import com.smartroomfinder.smartroomfinder.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacebookService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Transactional
    public AuthFacebookResponse processOAuthLogin(FacebookLoginRequest request) {
        log.info("Processing Facebook OAuth login for email: {}", request.getEmail());

        // 1. Tìm user theo Facebook ID
        Optional<Users> existingUserByFbId = userRepository.findByFacebookId(request.getFacebookId());

        if (existingUserByFbId.isPresent()) {
            log.info("Existing Facebook user found: {}", request.getEmail());
            Users user = existingUserByFbId.get();

            // Update thông tin nếu có thay đổi
            updateUserInfo(user, request);
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return generateAuthResponse(user);
        }

        Optional<Users> existingUserByEmail = userRepository.findByEmail(request.getEmail());

        if (existingUserByEmail.isPresent()) {
            log.info("User with email {} already exists, linking Facebook account", request.getEmail());
            Users user = existingUserByEmail.get();

            // Link Facebook account
            user.setFacebookId(request.getFacebookId());
            user.setLastLogin(LocalDateTime.now());

            // Update avatar nếu chưa có
            if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
                user.setAvatarUrl(request.getAvatarUrl());
            }

            userRepository.save(user);
            return generateAuthResponse(user);
        }

        log.info("Creating new user from Facebook: {}", request.getEmail());
        Users newUser = createNewFacebookUser(request);

        return generateAuthResponse(newUser);
    }

    private Users createNewFacebookUser(FacebookLoginRequest request) {
        // Lấy role mặc định (RENTER hoặc USER tùy theo yêu cầu)
        Roles userRole = roleRepository.findByRoleName("RENTER")
                .orElseThrow(() -> new ResourceNotFoundException("Role RENTER not found"));

        Users newUser = Users.builder()
                .email(request.getEmail())
                .username(generateUsername(request.getEmail()))
                .fullName(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .facebookId(request.getFacebookId())
                .authProvider("FACEBOOK")
                .isOAuthUser(true)
                .oauthEmailVerified(true)
                .passwordHash("") // OAuth users không cần password
                .role_id(userRole)
                .isActive(true)
                .isBanned(false)
                .identityVerified(false)
                .lastLogin(LocalDateTime.now())
                .build();

        Users savedUser = userRepository.save(newUser);
        log.info("New Facebook user created successfully: {}", savedUser.getEmail());

        return savedUser;
    }


    private void updateUserInfo(Users user, FacebookLoginRequest request) {
        boolean updated = false;

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl());
            updated = true;
            log.info("Updated avatar for user: {}", user.getEmail());
        }

        if (request.getName() != null && !request.getName().equals(user.getFullName())) {
            user.setFullName(request.getName());
            updated = true;
            log.info("Updated full name for user: {}", user.getEmail());
        }

        if (updated) {
            userRepository.save(user);
        }
    }


    private AuthFacebookResponse generateAuthResponse(Users user) {
        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(),
                user.getUserId().toString()
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getUsername(),
                user.getUserId().toString()
        );

        updateUserTokens(user, accessToken, refreshToken);

        log.info("Generated tokens for user: {}", user.getEmail());

        return AuthFacebookResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userMapper.toResponse(user))
                .message("Đăng nhập Facebook thành công")
                .build();
    }

    private void updateUserTokens(Users user, String accessToken, String refreshToken) {
        LocalDateTime now = LocalDateTime.now();

        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
        user.setAccessTokenExpiresAt(now.plusHours(24)); // 24 hours
        user.setRefreshTokenExpiresAt(now.plusDays(7));  // 7 days

        userRepository.save(user);
    }

    private String generateUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }
}