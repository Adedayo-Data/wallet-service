package com.hng.wallet_service.services;

import com.hng.wallet_service.models.User;
import com.hng.wallet_service.models.Wallet;
import com.hng.wallet_service.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public Wallet createWallet(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWalletNumber(generateWalletNumber());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("NGN");
        return walletRepository.save(wallet);
    }

    private String generateWalletNumber() {
        // Generate 10-digit wallet number
        Random random = new Random();
        StringBuilder walletNumber = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            walletNumber.append(random.nextInt(10));
        }

        // Check if it exists, regenerate if needed
        if (walletRepository.findByWalletNumber(walletNumber.toString()).isPresent()) {
            return generateWalletNumber();
        }

        return walletNumber.toString();
    }

    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user"));
    }

    public Wallet getWalletByWalletNumber(String walletNumber) {
        return walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }
}
