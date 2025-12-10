package com.hng.wallet_service.services;

import com.hng.wallet_service.exceptions.ApiKeyExpiredException;
import com.hng.wallet_service.exceptions.ApiKeyLimitExceededException;
import com.hng.wallet_service.exceptions.ApiKeyRevokedException;
import com.hng.wallet_service.exceptions.InvalidAmountException;
import com.hng.wallet_service.exceptions.UnauthorizedException;
import com.hng.wallet_service.exceptions.WalletNotFoundException;
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
            throw new ApiKeyLimitExceededException(
                    "Maximum of 5 active API keys allowed per user. Please revoke an existing key before creating a new one.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WalletNotFoundException("User not found with ID: " + userId));

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
                .orElseThrow(() -> new WalletNotFoundException("API key not found with ID: " + expiredKeyId));

        if (!expiredKey.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to rollover this API key");
        }

        if (expiredKey.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new InvalidAmountException("API key has not expired yet. Expires at: " + expiredKey.getExpiresAt());
        }

        // Create new key with same permissions
        return createApiKey(userId, expiredKey.getName(), expiredKey.getPermissions(), newExpiry);
    }

    public ApiKey validateApiKey(String rawKey) {
        // The rawKey passed here is the full key, e.g., "sk_live_abcdefgh..."
        // The prefix stored in the DB is "sk_live_" + first 8 chars of the generated
        // random key.
        // The hash stored in the DB is of the full generated random key (excluding
        // "sk_live_").

        // Extract the prefix from the raw key (first 16 chars: "sk_live_" + first 8
        // chars of random key)
        String keyPrefix = rawKey.substring(0, 16);

        // The actual random key part (without "sk_live_") starts at index 8 of the full
        // rawKey.
        String actualRandomKey = rawKey.substring(8);

        // Get ALL active API keys (not just ones with null userId!)
        List<ApiKey> keys = apiKeyRepository.findAll().stream()
                .filter(k -> k.getStatus() == ApiKeyStatus.ACTIVE)
                .toList();

        for (ApiKey key : keys) {
            if (key.getKeyPrefix().equals(keyPrefix)
                    && passwordEncoder.matches(rawKey.substring(8), key.getKeyHash())) {
                if (key.getExpiresAt().isBefore(LocalDateTime.now())) {
                    throw new ApiKeyExpiredException(
                            "API key has expired. Please create a new key or rollover the expired one.");
                }
                if (key.getStatus() != ApiKeyStatus.ACTIVE) {
                    throw new ApiKeyRevokedException("API key has been revoked. Please create a new key.");
                }
                return key;
            }
        }

        throw new UnauthorizedException("Invalid API key. Please check your key and try again.");
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
            default -> throw new InvalidAmountException("Invalid expiry format. Use 1H, 1D, 1M, or 1Y");
        };
    }

    public record ApiKeyCreationResponse(String apiKey, LocalDateTime expiresAt) {
    }
}
