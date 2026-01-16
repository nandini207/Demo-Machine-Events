package com.buyogo.demomachine_events.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.buyogo.demomachine_events.dto.BatchIngestResponse;
import com.buyogo.demomachine_events.dto.EventIngestRequest;
import com.buyogo.demomachine_events.dto.MachineStatsResponse;
import com.buyogo.demomachine_events.dto.TopDefectLineResponse;
import com.buyogo.demomachine_events.service.EventIngestService;

@RestController
@RequestMapping("/events")
public class EventIngestController {

    private final EventIngestService ingestService;

    public EventIngestController(EventIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchIngestResponse> ingestBatch(
            @RequestBody List<EventIngestRequest> events
    ) {
        BatchIngestResponse response = ingestService.ingestBatch(events);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/stats")
    public ResponseEntity<MachineStatsResponse> getStats(
        @RequestParam String machineId,
        @RequestParam Instant start,
        @RequestParam Instant end
    ) {
        return ResponseEntity.ok(
            ingestService.getMachineStats(machineId, start, end)
        );
    }
    @GetMapping("/stats/top-defect-lines")
    public ResponseEntity<List<TopDefectLineResponse>> topDefectLines(
        @RequestParam String factoryId,
        @RequestParam Instant from,
        @RequestParam Instant to,
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(
            ingestService.getTopDefectLines(from, to, limit)
        );
    }
}
