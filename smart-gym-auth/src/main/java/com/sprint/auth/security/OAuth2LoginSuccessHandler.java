package com.sprint.auth.security;


import com.sprint.auth.dto.response.AuthResponse;
import com.sprint.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    public OAuth2LoginSuccessHandler(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String login = oAuth2User.getAttribute("login");

        if (email == null) {
            email = login + "@github.com";
        }

        AuthResponse authResponse = authService.processOAuth2PostLogin(email, login);

        String frontendUrl = "http://localhost:3000/oauth2/redirect";

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("token", authResponse.accessToken())
                .queryParam("refreshToken", authResponse.refreshToken())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
