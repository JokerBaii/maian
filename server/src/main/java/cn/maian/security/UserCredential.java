package cn.maian.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
public class UserCredential {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @Column(length = 30, nullable = false, unique = true)
    private String phone;

    @Column(length = 100, nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserCredential() {
    }

    public UserCredential(UUID userId, String phone, String passwordHash, Instant createdAt) {
        this.userId = userId;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.enabled = true;
        this.createdAt = createdAt;
    }

    public UUID getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
}
