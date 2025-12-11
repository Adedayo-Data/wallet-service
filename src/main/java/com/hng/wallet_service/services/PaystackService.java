package com.hng.wallet_service.services;

import com.hng.wallet_service.models.Transaction;
import com.hng.wallet_service.models.Wallet;
import com.hng.wallet_service.models.enums.TransactionStatus;
import com.hng.wallet_service.models.enums.TransactionType;
import com.hng.wallet_service.repositories.TransactionRepository;
import com.hng.wallet_service.repositories.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaystackService {

    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    @Value("${paystack.webhook-secret}")
    private String webhookSecret;

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public Map<String, String> initializeDeposit(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Validate minimum amount Paystack requirement: minimum 100 Naira
        if (amount.compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new IllegalArgumentException("Minimum deposit amount is 100 Naira");
        }

        // Validate decimal precision (max 2 decimal places to avoid silent truncation)
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than 2 decimal places");
        }

        // Convert amount to kobo (Paystack uses kobo)
        long amountInKobo = amount.multiply(BigDecimal.valueOf(100)).longValue();

        // Create pending transaction
        String reference = "TXN_" + System.currentTimeMillis() + "_" + userId;
        Transaction transaction = new Transaction();
        transaction.setReference(reference);
        transaction.setWallet(wallet);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCounterpartWalletId(null);
        transactionRepository.save(transaction);

        // Call Paystack API
        String url = "https://api.paystack.co/transaction/initialize";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + paystackSecretKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> requestBody = Map.of(
                "email", wallet.getUser().getEmail(),
                "amount", amountInKobo,
                "reference", reference);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                Map.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");

        return Map.of(
                "reference", reference,
                "authorization_url", (String) data.get("authorization_url"));
    }

    @Transactional
    public void handleWebhook(String signature, String payload) {

        // Parse payload
        Map<String, Object> event = parsePayload(payload);
        String eventType = (String) event.get("event");

        if ("charge.success".equals(eventType)) {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String reference = (String) data.get("reference");
            String status = (String) data.get("status");

            // Find transaction
            Transaction transaction = transactionRepository.findByReference(reference)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Idempotency check
            if (transaction.getStatus() == TransactionStatus.SUCCESS) {
                return; // Already processed
            }

            if ("success".equals(status)) {
                // Update transaction status
                transaction.setStatus(TransactionStatus.SUCCESS);
                transactionRepository.save(transaction);

                // Credit wallet
                Wallet wallet = transaction.getWallet();
                wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
                walletRepository.save(wallet);
            } else {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
            }
        } else if ("charge.failed".equals(eventType)) {
            // Handle failed charge events
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String reference = (String) data.get("reference");

            // Find transaction
            Transaction transaction = transactionRepository.findByReference(reference)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Idempotency check - don't update if already processed
            if (transaction.getStatus() == TransactionStatus.SUCCESS ||
                    transaction.getStatus() == TransactionStatus.FAILED) {
                return; // Already processed
            }

            // Mark transaction as failed
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);

            // Log the failure for monitoring
            System.out.println("Transaction failed: " + reference);
        } else if ("charge.abandoned".equals(eventType)) {
            // Handle abandoned charge events (user closed payment page)
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String reference = (String) data.get("reference");

            // Find transaction
            Transaction transaction = transactionRepository.findByReference(reference)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Idempotency check - don't update if already processed
            if (transaction.getStatus() == TransactionStatus.SUCCESS ||
                    transaction.getStatus() == TransactionStatus.FAILED) {
                return; // Already processed
            }

            // Mark transaction as failed
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);

            // Log the abandonment for monitoring
            System.out.println("Transaction abandoned: " + reference);
        }
    }

    private boolean validateSignature(String signature, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }

    private Map<String, Object> parsePayload(String payload) {
        // Simple JSON parsing - in production use Jackson
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(payload, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse webhook payload");
        }
    }

    public Transaction getDepositStatus(String reference) {
        return transactionRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}
