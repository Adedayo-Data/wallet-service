package com.hng.wallet_service.repositories;

import com.hng.wallet_service.models.ApiKey;
import com.hng.wallet_service.models.enums.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    List<ApiKey> findByUserIdAndStatus(Long userId, ApiKeyStatus status);

    long countByUserIdAndStatus(Long userId, ApiKeyStatus status);
}
