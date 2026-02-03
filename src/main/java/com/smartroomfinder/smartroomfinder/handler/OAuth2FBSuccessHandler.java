package com.smartroomfinder.smartroomfinder.handler;

import com.smartroomfinder.smartroomfinder.services.AuthFacebookService;
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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FBSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthFacebookService authFacebookService;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        try {
            log.info("Facebook OAuth2 authentication successful");

            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String facebookId = attributes.get("id").toString();
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");

            // Lấy avatar URL từ Facebook
            String picture = null;
            if (attributes.get("picture") != null) {
                Map<String, Object> pictureObj = (Map<String, Object>) attributes.get("picture");
                if (pictureObj.get("data") != null) {
                    Map<String, Object> data = (Map<String, Object>) pictureObj.get("data");
                    picture = (String) data.get("url");
                }
            }

            log.info("Facebook User - ID: {}, Email: {}, Picture: {}", facebookId, email, picture);

            // Redirect về frontend với thông tin user (giống Google)
            String redirectUrl = frontendUrl + "/auth/callback?" +
                    "facebookId=" + facebookId +
                    "&email=" + (email != null ? email : "") +
                    "&name=" + (name != null ? name : "") +
                    "&picture=" + (picture != null ? picture : "");

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("Error in Facebook OAuth2 success handler: {}", e.getMessage());

            String errorUrl = frontendUrl + "/login?error=" + "oauth2_error";
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }
}