package com.hng.wallet_service.models;

import com.hng.wallet_service.models.enums.ApiKeyStatus;
import com.hng.wallet_service.models.enums.Permissions;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "apikey")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApiKey extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String keyHash;

    private String keyPrefix;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    private List<Permissions> permissions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiKeyStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
