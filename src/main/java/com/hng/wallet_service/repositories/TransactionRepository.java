package com.hng.wallet_service.repositories;

import com.hng.wallet_service.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByReference(String reference);

    List<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    boolean existsByReference(String reference);
}
