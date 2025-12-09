package com.hng.wallet_service.services;

import com.hng.wallet_service.models.Transaction;
import com.hng.wallet_service.models.Wallet;
import com.hng.wallet_service.models.enums.TransactionStatus;
import com.hng.wallet_service.models.enums.TransactionType;
import com.hng.wallet_service.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction createTransaction(
            String reference,
            Wallet wallet,
            TransactionType type,
            BigDecimal amount,
            TransactionStatus status,
            Long counterpartyWalletId) {
        Transaction transaction = new Transaction();
        transaction.setReference(reference);
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setStatus(status);
        transaction.setCounterpartWalletId(counterpartyWalletId);
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionHistory(Long walletId) {
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }

    public Transaction getTransactionByReference(String reference) {
        return transactionRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public boolean transactionExists(String reference) {
        return transactionRepository.existsByReference(reference);
    }
}
