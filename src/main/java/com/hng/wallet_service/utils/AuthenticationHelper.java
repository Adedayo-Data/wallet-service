package com.hng.wallet_service.utils;

import com.hng.wallet_service.models.User;
import com.hng.wallet_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationHelper {

    private final UserRepository userRepository;

    /**
     * Extracts userId from different authentication types:
     * - OAuth2User (from Google login) - looks up user by email
     * - Long (from JWT or API key) - returns directly
     */
    public Long getUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof Long) {
            // JWT or API Key authentication
            return (Long) principal;
        } else if (principal instanceof OAuth2User) {
            // OAuth2 authentication (Google login)
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return user.getId();
        }

        throw new RuntimeException("Unknown authentication type");
    }
}
