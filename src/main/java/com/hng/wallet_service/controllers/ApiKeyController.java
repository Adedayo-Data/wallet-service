package com.hng.wallet_service.controllers;

import com.hng.wallet_service.models.enums.Permissions;
import com.hng.wallet_service.services.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/create")
    public Map<String, Object> createApiKey(
            @RequestBody CreateApiKeyRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        ApiKeyService.ApiKeyCreationResponse response = apiKeyService.createApiKey(
                userId,
                request.name(),
                request.permissions(),
                request.expiry());

        return Map.of(
                "api_key", response.apiKey(),
                "expires_at", response.expiresAt().toString());
    }

    @PostMapping("/rollover")
    public Map<String, Object> rolloverApiKey(
            @RequestBody RolloverRequest request,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        ApiKeyService.ApiKeyCreationResponse response = apiKeyService.rolloverApiKey(
                userId,
                request.expiredKeyId(),
                request.expiry());

        return Map.of(
                "api_key", response.apiKey(),
                "expires_at", response.expiresAt().toString());
    }

    public record CreateApiKeyRequest(String name, List<Permissions> permissions, String expiry) {
    }

    public record RolloverRequest(Long expiredKeyId, String expiry) {
    }
}
