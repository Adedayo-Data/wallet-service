package com.hng.wallet_service.controllers;

import com.hng.wallet_service.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/google")
    public Map<String, String> initiateGoogleLogin(HttpServletRequest request) {
        // Get the base URL (works for both localhost and Render)
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }

        String oauthUrl = baseUrl + "/oauth2/authorization/google";

        return Map.of(
                "message", "Click the URL below to login with Google",
                "google_login_url", oauthUrl);
    }

    @GetMapping("/google/callback")
    public Map<String, String> googleCallback(
            @AuthenticationPrincipal OAuth2User oauth2User,
            HttpServletResponse response) throws IOException {
        if (oauth2User == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authorization code");
            return null;
        }

        String jwt = authService.handleGoogleLogin(oauth2User);

        Map<String, String> result = new HashMap<>();
        result.put("token", jwt);
        result.put("message", "Login successful");

        return result;
    }
}
