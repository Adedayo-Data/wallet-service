package com.hng.wallet_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Wallet extends BaseEntity{

    @Column(unique = true, nullable = false)
    private String walletNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
    private String currency = "NGN";

    @OneToOne
    @JoinColumn(name = "user_id", nullable=false)
    private User user;

    @Version
    private Long version;
}
