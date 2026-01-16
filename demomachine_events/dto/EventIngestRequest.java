package com.buyogo.demomachine_events.dto;

import java.time.Instant;

public class EventIngestRequest {

    public String eventId;
    public Instant eventTime;
    public String machineId;
    public long durationMs;
    public int defectCount;
}