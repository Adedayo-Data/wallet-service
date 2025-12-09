package com.hng.wallet_service.services;

import com.hng.wallet_service.models.ApiKey;
import com.hng.wallet_service.models.User;
import com.hng.wallet_service.models.enums.ApiKeyStatus;
import com.hng.wallet_service.models.enums.Permissions;
import com.hng.wallet_service.repositories.ApiKeyRepository;
import com.hng.wallet_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public ApiKeyCreationResponse createApiKey(
            Long userId,
            String name,
            List<Permissions> permissions,
            String expiry) {
        // Check 5-key limit
        long activeKeyCount = apiKeyRepository.countByUserIdAndStatus(userId, ApiKeyStatus.ACTIVE);
        if (activeKeyCount >= 5) {
            throw new RuntimeException("Maximum 5 active API keys allowed per user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate random API key
        String rawKey = generateRandomKey();
        String keyHash = passwordEncoder.encode(rawKey);
        String keyPrefix = "sk_live_" + rawKey.substring(0, 8);

        // Convert expiry to LocalDateTime
        LocalDateTime expiresAt = calculateExpiryDate(expiry);

        ApiKey apiKey = new ApiKey();
        apiKey.setUser(user);
        apiKey.setName(name);
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setPermissions(permissions);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setExpiresAt(expiresAt);

        apiKeyRepository.save(apiKey);

        return new ApiKeyCreationResponse("sk_live_" + rawKey, expiresAt);
    }

    @Transactional
    public ApiKeyCreationResponse rolloverApiKey(Long userId, Long expiredKeyId, String newExpiry) {
        ApiKey expiredKey = apiKeyRepository.findById(expiredKeyId)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        if (!expiredKey.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (expiredKey.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Key is not expired yet");
        }

        // Create new key with same permissions
        return createApiKey(userId, expiredKey.getName(), expiredKey.getPermissions(), newExpiry);
    }

    public ApiKey validateApiKey(String rawKey) {
        String keyPrefix = "sk_live_" + rawKey.substring(8, 16);

        List<ApiKey> keys = apiKeyRepository.findByUserIdAndStatus(null, ApiKeyStatus.ACTIVE);

        for (ApiKey key : keys) {
            if (key.getKeyPrefix().equals(keyPrefix)
                    && passwordEncoder.matches(rawKey.substring(8), key.getKeyHash())) {
                if (key.getExpiresAt().isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("API key expired");
                }
                if (key.getStatus() != ApiKeyStatus.ACTIVE) {
                    throw new RuntimeException("API key revoked");
                }
                return key;
            }
        }

        throw new RuntimeException("Invalid API key");
    }

    private String generateRandomKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private LocalDateTime calculateExpiryDate(String expiry) {
        LocalDateTime now = LocalDateTime.now();

        return switch (expiry) {
            case "1H" -> now.plusHours(1);
            case "1D" -> now.plusDays(1);
            case "1M" -> now.plusMonths(1);
            case "1Y" -> now.plusYears(1);
            default -> throw new RuntimeException("Invalid expiry format. Use 1H, 1D, 1M, or 1Y");
        };
    }

    public record ApiKeyCreationResponse(String apiKey, LocalDateTime expiresAt) {
    }
}
