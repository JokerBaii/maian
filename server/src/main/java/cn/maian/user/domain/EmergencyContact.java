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
@Table(name = "emergency_contacts")
public class EmergencyContact {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, nullable = false)
    private UUID userId;

    @Column(length = 60, nullable = false)
    private String name;

    @Column(length = 30, nullable = false)
    private String phone;

    @Column(name = "relation_name", length = 30, nullable = false)
    private String relation;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected EmergencyContact() {
    }

    public static EmergencyContact create(UUID userId, String name, String phone, String relation) {
        var contact = new EmergencyContact();
        contact.id = UUID.randomUUID();
        contact.userId = userId;
        contact.createdAt = Instant.now();
        contact.update(name, phone, relation);
        return contact;
    }

    public void update(String name, String phone, String relation) {
        this.name = name;
        this.phone = phone;
        this.relation = relation;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getRelation() {
        return relation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
