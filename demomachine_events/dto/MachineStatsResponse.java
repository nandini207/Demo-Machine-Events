package com.buyogo.demomachine_events.dto;

import java.time.Instant;

public class MachineStatsResponse {

    public String machineId;
    public Instant start;
    public Instant end;

    public long eventsCount;
    public long defectsCount;
    public double avgDefectRate;
    public String status;
}
