package com.smartroomfinder.smartroomfinder.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
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

    private static final String GOOGLE_TOKENINFO_URL =
            "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=";

    private static final String GOOGLE_USERINFO_URL =
            "https://www.googleapis.com/oauth2/v3/userinfo";

    @Transactional
    public AuthGoogleResponse loginWithGoogle(String accessToken)
            throws GeneralSecurityException, IOException {
        log.info("🔐 Processing Google login with access_token...");

        GoogleUserInfo userInfo = verifyAndGetUserInfo(accessToken);

        log.info("Google token verified - Email: {}, GoogleId: {}", userInfo.email, userInfo.googleId);

        Users user = findOrCreateUser(
                userInfo.googleId,
                userInfo.email,
                userInfo.fullName,
                userInfo.picture,
                userInfo.emailVerified
        );

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String jwtAccessToken = jwtUtil.generateAccessToken(
                user.getUsername(),
                user.getUserId().toString(),
                user.getTokenVersion()
        );

        String jwtRefreshToken = jwtUtil.generateRefreshToken(
                user.getUsername(),
                user.getUserId().toString(),
                user.getTokenVersion()
        );

        user.setAccessToken(jwtAccessToken);
        user.setRefreshToken(jwtRefreshToken);
        user.setAccessTokenExpiresAt(LocalDateTime.now().plusHours(1));
        user.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        log.info("User authenticated successfully - UserId: {}", user.getUserId());

        UserResponse userResponse = userMapper.toResponse(user);
        return AuthGoogleResponse.builder()
                .accessToken(jwtAccessToken)
                .refreshToken(jwtRefreshToken)
                .user(userResponse)
                .message("Login successful")
                .build();
    }


    private GoogleUserInfo verifyAndGetUserInfo(String accessToken) throws IOException {
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest tokenInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_TOKENINFO_URL + accessToken))
                .GET()
                .build();

        HttpResponse<String> tokenInfoResponse;
        try {
            tokenInfoResponse = httpClient.send(tokenInfoRequest,
                    HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while verifying Google token", e);
        }

        if (tokenInfoResponse.statusCode() != 200) {
            log.error("Google tokeninfo returned {}: {}", tokenInfoResponse.statusCode(),
                    tokenInfoResponse.body());
            throw new RuntimeException("Invalid or expired Google access token");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode tokenInfo = mapper.readTree(tokenInfoResponse.body());

        JsonNode expiresIn = tokenInfo.get("expires_in");
        if (expiresIn == null || expiresIn.asInt() <= 0) {
            throw new RuntimeException("Google access token has expired");
        }

        HttpRequest userInfoRequest = HttpRequest.newBuilder()
                .uri(URI.create(GOOGLE_USERINFO_URL))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> userInfoResponse;
        try {
            userInfoResponse = httpClient.send(userInfoRequest,
                    HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching Google user info", e);
        }

        if (userInfoResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch Google user info");
        }

        JsonNode userInfo = mapper.readTree(userInfoResponse.body());

        return new GoogleUserInfo(
                userInfo.path("sub").asText(),          // googleId
                userInfo.path("email").asText(),
                userInfo.path("name").asText(null),
                userInfo.path("picture").asText(null),
                userInfo.path("email_verified").asBoolean(false)
        );
    }

    private record GoogleUserInfo(
            String googleId,
            String email,
            String fullName,
            String picture,
            boolean emailVerified
    ) {}


    private Users findOrCreateUser(String googleId, String email, String fullName,
                                   String picture, boolean emailVerified) {

        Optional<Users> existingUser = userRepository.findByGoogleId(googleId);
        if (existingUser.isPresent()) {
            log.info("User found by Google ID: {}", googleId);
            Users user = existingUser.get();
            if (picture != null && !picture.isEmpty()) user.setAvatarUrl(picture);
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
            if (picture != null && !picture.isEmpty()) user.setAvatarUrl(picture);
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
                .tokenVersion(0)
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
            username = baseUsername + counter++;
        }
        return username;
    }

    @Transactional
    public void logout(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        String userId = jwtUtil.getUserIdFromToken(token);
        UUID parsedUserId = UUID.fromString(userId);

        Users user = userRepository.findById(parsedUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("User logged out. Token version increased - UserId: {}", userId);
    }
}