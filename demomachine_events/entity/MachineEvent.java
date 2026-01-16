package com.buyogo.demomachine_events.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "machine_event",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "event_id")
    }
)
public class MachineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "received_time", nullable = false)
    private Instant receivedTime;

    @Column(name = "machine_id", nullable = false)
    private String machineId;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "defect_count", nullable = false)
    private int defectCount;

    @Column(name = "payload_hash", nullable = false)
    private String payloadHash;

    protected MachineEvent() {
        // JPA requirement
    }

    public MachineEvent(
            String eventId,
            Instant eventTime,
            Instant receivedTime,
            String machineId,
            long durationMs,
            int defectCount,
            String payloadHash
    ) {
        this.eventId = eventId;
        this.eventTime = eventTime;
        this.receivedTime = receivedTime;
        this.machineId = machineId;
        this.durationMs = durationMs;
        this.defectCount = defectCount;
        this.payloadHash = payloadHash;
    }

    // All Getters

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public Instant getReceivedTime() {
        return receivedTime;
    }

    public String getMachineId() {
        return machineId;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getDefectCount() {
        return defectCount;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    // Updating  helpers here

    public void updateFrom(MachineEvent newer) {
        this.eventTime = newer.eventTime;
        this.receivedTime = newer.receivedTime;
        this.machineId = newer.machineId;
        this.durationMs = newer.durationMs;
        this.defectCount = newer.defectCount;
        this.payloadHash = newer.payloadHash;
    }

    public boolean hasSamePayload(MachineEvent other) {
        return Objects.equals(this.payloadHash, other.payloadHash);
    }
}
