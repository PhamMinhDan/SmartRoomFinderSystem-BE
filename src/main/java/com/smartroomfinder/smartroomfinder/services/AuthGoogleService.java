package com.smartroomfinder.smartroomfinder.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.smartroomfinder.smartroomfinder.dto.response.AuthGoogleResponse;
import com.smartroomfinder.smartroomfinder.dto.response.UserResponse;
import com.smartroomfinder.smartroomfinder.entities.Roles;
import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.mappers.UserMapper;
import com.smartroomfinder.smartroomfinder.repositories.RoleRepository;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import com.smartroomfinder.smartroomfinder.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthGoogleService {

    private final UserRepository userRepository;
    private final RoleRepository rolesRepository;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Transactional
    public AuthGoogleResponse loginWithGoogle(String idTokenString) throws GeneralSecurityException, IOException {
        log.info("🔐 Processing Google login...");

        // Xác minh ID Token từ Google
        GoogleIdToken idToken = verifyGoogleToken(idTokenString);
        if (idToken == null) {
            log.error("Invalid Google ID Token");
            throw new RuntimeException("Invalid Google ID Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String fullName = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        boolean emailVerified = payload.getEmailVerified();

        log.info("Google token verified - Email: {}, GoogleId: {}", email, googleId);

        Users user = findOrCreateUser(googleId, email, fullName, picture, emailVerified);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getUserId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getUserId().toString());

        user.setAccessToken(accessToken);
        user.setRefreshToken(refreshToken);
        user.setAccessTokenExpiresAt(LocalDateTime.now().plusHours(1));
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        log.info("User authenticated successfully - UserId: {}", user.getUserId());

        // Tạo response
        UserResponse userResponse = userMapper.toResponse(user);
        return AuthGoogleResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
//                .userId(user.getUserId().toString())
                .user(userResponse)
                .message("Login successful")
                .build();
    }

    private GoogleIdToken verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            return verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            log.error("Error verifying Google token: {}", e.getMessage());
            return null;
        }
    }


    private Users findOrCreateUser(String googleId, String email, String fullName,
                                   String picture, boolean emailVerified) {

        Optional<Users> existingUser = userRepository.findByGoogleId(googleId);
        if (existingUser.isPresent()) {
            log.info(" User found by Google ID: {}", googleId);
            Users user = existingUser.get();

            if (picture != null && !picture.isEmpty()) {
                user.setAvatarUrl(picture);
            }
            user.setOauthEmailVerified(emailVerified);

            return user;
        }

        Optional<Users> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            log.info("User found by email: {}", email);
            Users user = userByEmail.get();

            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                user.setAuthProvider("GOOGLE");
                user.setIsOAuthUser(true);
            }

            if (picture != null && !picture.isEmpty()) {
                user.setAvatarUrl(picture);
            }
            user.setOauthEmailVerified(emailVerified);

            return user;
        }

        log.info("Creating new user from Google - Email: {}", email);

        Roles userRole = rolesRepository.findByRoleName("RENTER")
                .orElseThrow(() -> new RuntimeException("RENTER role not found"));

        Users newUser = Users.builder()
                .username(generateUsername(email))
                .email(email)
                .fullName(fullName != null ? fullName : email.split("@")[0])
                .googleId(googleId)
                .authProvider("GOOGLE")
                .avatarUrl(picture)
                .isOAuthUser(true)
                .oauthEmailVerified(emailVerified)
                .passwordHash("")
                .isActive(true)
                .isBanned(false)
                .identityVerified(false)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .role_id(userRole)
                .build();

        Users savedUser = userRepository.save(newUser);
        log.info("New user created - UserId: {}, Email: {}", savedUser.getUserId(), email);

        return savedUser;
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

    @Transactional
    public void logout(String userId) {
        try {
            UUID parsedUserId = UUID.fromString(userId);
            Optional<Users> userOpt = userRepository.findById(parsedUserId);

            if (userOpt.isPresent()) {
                Users user = userOpt.get();
                user.setAccessToken(null);
                user.setRefreshToken(null);
                user.setAccessTokenExpiresAt(null);
                user.setRefreshTokenExpiresAt(null);
                userRepository.save(user);
                log.info("User logout successfully - UserId: {}", userId);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format: {}", userId);
        }
    }
}