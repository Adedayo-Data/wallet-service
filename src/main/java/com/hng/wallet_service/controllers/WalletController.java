package com.hng.wallet_service.controllers;

import com.hng.wallet_service.models.Transaction;
import com.hng.wallet_service.models.Wallet;
import com.hng.wallet_service.services.PaystackService;
import com.hng.wallet_service.services.TransferService;
import com.hng.wallet_service.services.WalletService;
import com.hng.wallet_service.services.TransactionService;
import com.hng.wallet_service.utils.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final PaystackService paystackService;
    private final TransferService transferService;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final AuthenticationHelper authHelper;

    @PostMapping("/deposit")
    public Map<String, String> deposit(
            @RequestBody DepositRequest request,
            Authentication authentication) {
        Long userId = authHelper.getUserId(authentication);
        return paystackService.initializeDeposit(userId, request.amount());
    }

    @PostMapping("/paystack/webhook")
    public ResponseEntity<Map<String, Boolean>> paystackWebhook(
            @RequestHeader("x-paystack-signature") String signature,
            @RequestBody String payload) {
        paystackService.handleWebhook(signature, payload);
        return ResponseEntity.ok(Map.of("status", true));
    }

    @GetMapping("/deposit/{reference}/status")
    public Map<String, Object> getDepositStatus(@PathVariable String reference) {
        Transaction transaction = paystackService.getDepositStatus(reference);

        return Map.of(
                "reference", transaction.getReference(),
                "status", transaction.getStatus().name().toLowerCase(),
                "amount", transaction.getAmount());
    }

    @PostMapping("/transfer")
    public Map<String, String> transfer(
            @RequestBody TransferRequest request,
            Authentication authentication) {
        Long userId = authHelper.getUserId(authentication);
        transferService.transfer(userId, request.walletNumber(), request.amount());

        return Map.of(
                "status", "success",
                "message", "Transfer completed");
    }

    @GetMapping("/balance")
    public Map<String, BigDecimal> getBalance(Authentication authentication) {
        Long userId = authHelper.getUserId(authentication);
        Wallet wallet = walletService.getWalletByUserId(userId);

        return Map.of("balance", wallet.getBalance());
    }

    @GetMapping("/transactions")
    public java.util.List<Map<String, Object>> getTransactions(Authentication authentication) {
        Long userId = authHelper.getUserId(authentication);
        Wallet wallet = walletService.getWalletByUserId(userId);
        java.util.List<Transaction> transactions = transactionService.getTransactionHistory(wallet.getId());

        return transactions.stream()
                .map(txn -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("type", txn.getType().name().toLowerCase());
                    map.put("amount", txn.getAmount());
                    map.put("status", txn.getStatus().name().toLowerCase());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public record DepositRequest(BigDecimal amount) {
    }

    public record TransferRequest(String walletNumber, BigDecimal amount) {
    }
}
