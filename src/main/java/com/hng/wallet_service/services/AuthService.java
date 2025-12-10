package com.hng.wallet_service.services;

import com.hng.wallet_service.dto.AuthResponseDTO;
import com.hng.wallet_service.models.User;
import com.hng.wallet_service.models.Wallet;
import com.hng.wallet_service.repositories.UserRepository;
import com.hng.wallet_service.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponseDTO handleGoogleLogin(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String googleSubjectId = oauth2User.getAttribute("sub");

        // Find or create user
        User user = userRepository.findByGoogleSubjectId(googleSubjectId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(name);
                    newUser.setGoogleSubjectId(googleSubjectId);
                    User savedUser = userRepository.save(newUser);

                    // Auto-create wallet for new user
                    walletService.createWallet(savedUser);

                    return savedUser;
                });

        // Get user's wallet
        Wallet wallet = walletService.getWalletByUserId(user.getId());

        // Generate JWT
        String jwt = jwtUtil.generateToken(user.getEmail(), user.getId());

        // Build and return response
        return AuthResponseDTO.builder()
                .token(jwt)
                .message("Login successful")
                .walletNumber(wallet.getWalletNumber())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }
}
