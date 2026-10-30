package com.nexturn.lms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Transactional outbox record containing only optional employee ID and operation, never profile/password data. */
@Entity @Table(name="employee_event")
public class EmployeeEvent {
    @Id @Column(length=36) private String eventId;
    @Column private Integer employeeId;
    @Column(nullable=false,length=20) private String eventType;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private boolean published;
    protected EmployeeEvent() { }
    public EmployeeEvent(Integer employeeId, String eventType) {
        this.eventId=UUID.randomUUID().toString(); this.employeeId=employeeId;
        this.eventType=eventType; this.createdAt=Instant.now(); this.published=false;
    }
    public String getEventId() { return eventId; }
    public Integer getEmployeeId() { return employeeId; }
    public String getEventType() { return eventType; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isPublished() { return published; }
}
