package com.smartroomfinder.smartroomfinder.securities;

import com.smartroomfinder.smartroomfinder.entities.Users;
import com.smartroomfinder.smartroomfinder.repositories.UserRepository;
import com.smartroomfinder.smartroomfinder.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = getJwtFromRequest(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtil.extractAllClaims(token);

            String username = claims.get("username", String.class);
            String userId = claims.get("userId", String.class);
            Integer tokenVersionFromToken = claims.get("tokenVersion", Integer.class);

            Users user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!tokenVersionFromToken.equals(user.getTokenVersion())) {
                log.warn("Token version mismatch for user: {}", username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            // ✅ FIX: Build authorities từ user roles
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            if (user.getRole_id() != null) {
                String roleName = user.getRole_id().getRoleName();
                // ✅ Đảm bảo có tiền tố ROLE_
                String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                authorities.add(new SimpleGrantedAuthority(authority));
                log.debug("✅ Added authority: {}", authority);
            } else {
                log.warn("⚠️  User {} has no role assigned", username);
            }

            // ✅ FIX: Set credentials = userId (quan trọng cho extractUserId())
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,           // principal
                            userId,             // credentials (quan trọng!)
                            authorities         // authorities (quan trọng!)
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("✅ JWT authenticated - User: {}, Role: {}, Authorities: {}",
                    username,
                    user.getRole_id() != null ? user.getRole_id().getRoleName() : "NONE",
                    authorities);

        } catch (ExpiredJwtException e) {
            log.debug("❌ Access token expired");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (Exception e) {
            log.error("❌ Error in JWT filter: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return null;
        }

        String token = bearerToken.substring(7);

        if (token.isBlank()
                || token.equals("null")
                || token.equals("undefined")
                || token.split("\\.").length != 3) {
            return null;
        }

        return token;
    }
}