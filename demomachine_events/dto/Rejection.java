package com.buyogo.demomachine_events.dto;

public class Rejection {
    public String eventId;
    public String reason;

    public Rejection(String eventId, String reason) {
        this.eventId = eventId;
        this.reason = reason;
    }
}
