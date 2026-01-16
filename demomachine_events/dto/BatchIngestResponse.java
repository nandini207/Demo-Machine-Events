package com.buyogo.demomachine_events.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchIngestResponse {
    public int accepted;
    public int deduped;
    public int updated;
    public int rejected;

    public List<Rejection> rejections = new ArrayList<>();
}
