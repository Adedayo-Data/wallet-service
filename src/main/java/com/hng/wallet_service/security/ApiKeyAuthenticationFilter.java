package com.hng.wallet_service.security;

import com.hng.wallet_service.models.ApiKey;
import com.hng.wallet_service.models.enums.Permissions;
import com.hng.wallet_service.services.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String apiKeyHeader = request.getHeader("x-api-key");
        System.out.println("DEBUG: x-api-key header = " + (apiKeyHeader != null ? "present" : "null"));

        if (apiKeyHeader != null) {
            try {
                ApiKey apiKey = apiKeyService.validateApiKey(apiKeyHeader);

                // Convert permissions to authorities
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_SERVICE"));

                for (Permissions permission : apiKey.getPermissions()) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + permission.name()));
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        apiKey.getUser().getId(),
                        null,
                        authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Invalid API key, continue without authentication
                System.err.println("API Key validation failed: " + e.getMessage());
                e.printStackTrace();
            }
        }

        filterChain.doFilter(request, response);
    }
}
