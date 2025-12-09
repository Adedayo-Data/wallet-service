package com.hng.wallet_service.models;

import com.hng.wallet_service.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        // Application started successfully
        System.out.println("Wallet Service Started Successfully!");
    }
}
