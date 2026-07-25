package cn.maian.user.dto;

import cn.maian.user.domain.EmergencyContact;

import java.time.Instant;
import java.util.UUID;

public record EmergencyContactResponse(
    UUID id,
    String name,
    String phone,
    String relation,
    Instant createdAt
) {
    public static EmergencyContactResponse from(EmergencyContact contact) {
        return new EmergencyContactResponse(
            contact.getId(),
            contact.getName(),
            contact.getPhone(),
            contact.getRelation(),
            contact.getCreatedAt()
        );
    }
}
