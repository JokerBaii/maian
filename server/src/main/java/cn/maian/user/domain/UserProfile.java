package cn.maian.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(length = 60, nullable = false)
    private String nickname;

    @Column(length = 30, nullable = false)
    private String phone;

    @Column(length = 30, nullable = false)
    private String role;

    @Column(length = 60)
    private String realName;

    @Column(length = 30)
    private String idCardMasked;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserProfile() {
    }

    public static UserProfile register(UUID id, String nickname, String phone, Instant now) {
        UserProfile profile = new UserProfile();
        profile.id = id;
        profile.nickname = nickname;
        profile.phone = phone;
        profile.role = "USER";
        profile.verified = false;
        profile.createdAt = now;
        return profile;
    }

    public void verify(String realName, String idCardMasked) {
        this.realName = realName;
        this.idCardMasked = idCardMasked;
        this.verified = true;
    }

    public UUID getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getRealName() {
        return realName;
    }

    public String getIdCardMasked() {
        return idCardMasked;
    }

    public boolean isVerified() {
        return verified;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
