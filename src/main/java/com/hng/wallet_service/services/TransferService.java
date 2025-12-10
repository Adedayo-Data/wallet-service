package com.hng.wallet_service.services;

import com.hng.wallet_service.exceptions.InsufficientBalanceException;
import com.hng.wallet_service.exceptions.InvalidAmountException;
import com.hng.wallet_service.exceptions.WalletNotFoundException;
import com.hng.wallet_service.models.Wallet;
import com.hng.wallet_service.models.enums.TransactionStatus;
import com.hng.wallet_service.models.enums.TransactionType;
import com.hng.wallet_service.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferService {

        private final WalletRepository walletRepository;
        private final TransactionService transactionService;

        @Transactional
        public void transfer(Long senderUserId, String recipientWalletNumber, BigDecimal amount) {
                // Validate amount
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidAmountException("Amount must be greater than zero");
                }

                // Get sender wallet with lock
                Wallet senderWallet = walletRepository.findByUserId(senderUserId)
                                .orElseThrow(() -> new WalletNotFoundException("Sender wallet not found"));

                // Lock sender wallet to prevent race conditions
                Wallet lockedSenderWallet = walletRepository.findByIdWithLock(senderWallet.getId())
                                .orElseThrow(() -> new WalletNotFoundException("Failed to lock sender wallet"));

                // Check balance
                if (lockedSenderWallet.getBalance().compareTo(amount) < 0) {
                        throw new InsufficientBalanceException("Insufficient balance. Available: "
                                        + lockedSenderWallet.getBalance() + ", Required: " + amount);
                }

                // Get recipient wallet with lock
                Wallet recipientWallet = walletRepository.findByWalletNumberWithLock(recipientWalletNumber)
                                .orElseThrow(() -> new WalletNotFoundException(
                                                "Recipient wallet not found: " + recipientWalletNumber));

                // Generate unique reference
                String reference = "TRANSFER_" + System.currentTimeMillis() + "_" + senderUserId;

                // Debit sender
                lockedSenderWallet.setBalance(lockedSenderWallet.getBalance().subtract(amount));
                walletRepository.save(lockedSenderWallet);

                // Create TRANSFER_OUT transaction for sender
                transactionService.createTransaction(
                                reference + "_OUT",
                                lockedSenderWallet,
                                TransactionType.TRANSFER_OUT,
                                amount,
                                TransactionStatus.SUCCESS,
                                recipientWallet.getId());

                // Credit recipient
                recipientWallet.setBalance(recipientWallet.getBalance().add(amount));
                walletRepository.save(recipientWallet);

                // Create TRANSFER_IN transaction for recipient
                transactionService.createTransaction(
                                reference + "_IN",
                                recipientWallet,
                                TransactionType.TRANSFER_IN,
                                amount,
                                TransactionStatus.SUCCESS,
                                lockedSenderWallet.getId());
        }
}
