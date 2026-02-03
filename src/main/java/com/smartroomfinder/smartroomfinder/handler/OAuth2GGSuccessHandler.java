package com.smartroomfinder.smartroomfinder.handler;

import com.smartroomfinder.smartroomfinder.services.AuthGoogleService;
import com.smartroomfinder.smartroomfinder.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2GGSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthGoogleService oauth2Service;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        try {
            log.info("OAuth2 authentication successful");

            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String googleId = oAuth2User.getName();
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");

            log.info("OAuth2 User - GoogleId: {}, Email: {}", googleId, email);

            String redirectUrl = frontendUrl + "/auth/callback?" +
                    "googleId=" + googleId +
                    "&email=" + email +
                    "&name=" + (name != null ? name : "") +
                    "&picture=" + (picture != null ? picture : "");

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("Error in OAuth2 success handler: {}", e.getMessage());

            String errorUrl = frontendUrl + "/login?error=" + "oauth2_error";
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }
}