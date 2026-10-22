package cn.maian.agent.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="assistant_reminders", indexes={@Index(name="idx_reminder_owner",columnList="owner_id,status"),@Index(name="idx_reminder_check",columnList="status,next_check_at")})
public class AssistantReminder {
    @Id @JdbcTypeCode(SqlTypes.CHAR) @Column(length=36) private UUID id;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name="owner_id",length=36,nullable=false) private UUID ownerId;
    @Column(length=120,nullable=false) private String title;
    @Column(length=24,nullable=false) private String kind;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(length=36) private UUID targetId;
    @Column(length=20,nullable=false) private String status;
    @Column(length=300) private String result;
    private Instant dueAt;
    @Column(name="next_check_at",nullable=false) private Instant nextCheckAt;
    @Column(nullable=false) private Instant createdAt;
    private Instant triggeredAt;
    @Version private long version;
    protected AssistantReminder() {}
    public static AssistantReminder create(UUID owner,String title,String kind,UUID targetId,Instant dueAt,Instant now) {
        var reminder=new AssistantReminder(); reminder.id=UUID.randomUUID();reminder.ownerId=owner;reminder.title=title;reminder.kind=kind;reminder.targetId=targetId;reminder.dueAt=dueAt;reminder.createdAt=now;reminder.status="WAITING";reminder.nextCheckAt=dueAt==null?now:dueAt;return reminder;
    }
    public void checked(Instant now,String message) {
        if (!"WAITING".equals(status)) return;
        nextCheckAt=now.plusSeconds(30);
        if(message!=null){status="READY";result=message;triggeredAt=now;}
    }
    public void dismiss(){status="DISMISSED";}
    public UUID getId(){return id;} public UUID getOwnerId(){return ownerId;} public String getTitle(){return title;} public String getKind(){return kind;} public UUID getTargetId(){return targetId;} public String getStatus(){return status;} public String getResult(){return result;} public Instant getDueAt(){return dueAt;} public Instant getNextCheckAt(){return nextCheckAt;} public Instant getCreatedAt(){return createdAt;} public Instant getTriggeredAt(){return triggeredAt;}
}
