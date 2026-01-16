package com.buyogo.demomachine_events.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buyogo.demomachine_events.dto.BatchIngestResponse;
import com.buyogo.demomachine_events.dto.EventIngestRequest;
import com.buyogo.demomachine_events.dto.MachineStatsResponse;
import com.buyogo.demomachine_events.dto.Rejection;
import com.buyogo.demomachine_events.dto.TopDefectLineResponse;
import com.buyogo.demomachine_events.entity.MachineEvent;
import com.buyogo.demomachine_events.repository.MachineEventRepository;

@Service
public class EventIngestService {

    private static final long MAX_DURATION_MS = Duration.ofHours(6).toMillis();
    private static final Duration FUTURE_EVENT_LIMIT = Duration.ofMinutes(15);

    private final MachineEventRepository repository;

    public EventIngestService(MachineEventRepository repository) {
        this.repository = repository;
    }

    
    @Transactional
    public BatchIngestResponse ingestBatch(List<EventIngestRequest> requests) {

        BatchIngestResponse response = new BatchIngestResponse();
        Instant now = Instant.now();

        for (EventIngestRequest req : requests) {

        
            String validationError = validate(req, now);
            if (validationError != null) {
                response.rejected++;
                response.rejections.add(new Rejection(req.eventId, validationError));
                continue;
            }

            Instant receivedTime = Instant.now();
            String payloadHash = computePayloadHash(req);

            MachineEvent incoming = new MachineEvent(
                    req.eventId,
                    req.eventTime,
                    receivedTime,
                    req.machineId,
                    req.durationMs,
                    req.defectCount,
                    payloadHash
            );


            Optional<MachineEvent> existingOpt = repository.findByEventId(req.eventId);

            if (existingOpt.isEmpty()) {
                repository.save(incoming);
                response.accepted++;
                continue;
            }

            MachineEvent existing = existingOpt.get();

            if (existing.hasSamePayload(incoming)) {
                response.deduped++;
                continue;
            }

            if (incoming.getReceivedTime().isAfter(existing.getReceivedTime())) {
                existing.updateFrom(incoming);
                repository.save(existing);
                response.updated++;
            } else {
                response.deduped++;
            }
        }

        return response;
    }


    private String validate(EventIngestRequest req, Instant now) {

        if (req.durationMs < 0 || req.durationMs > MAX_DURATION_MS) {
            return "INVALID_DURATION";
        }

        if (req.eventTime.isAfter(now.plus(FUTURE_EVENT_LIMIT))) {
            return "EVENT_TIME_IN_FUTURE";
        }

        return null;
    }

    private String computePayloadHash(EventIngestRequest req) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String raw = req.eventId
                    + req.eventTime
                    + req.machineId
                    + req.durationMs
                    + req.defectCount;

            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to compute payload hash", e);
        }
    }
    @Transactional(readOnly = true)
    public MachineStatsResponse getMachineStats(
            String machineId,
            Instant start,
            Instant end
     ) { 
        long eventsCount = repository.countEventsForMachineInWindow(machineId, start, end);
        long defectsCount = repository.sumDefectsForMachineInWindow(machineId, start, end);
        double windowHours =(end.toEpochMilli() - start.toEpochMilli()) / 3600000.0;
        double avgDefectRate = windowHours > 0 ? defectsCount / windowHours : 0.0;
        String status = avgDefectRate < 2.0 ? "Healthy" : "Warning";

        MachineStatsResponse response = new MachineStatsResponse();
        response.machineId = machineId;
        response.start = start;
        response.end = end;
        response.eventsCount = eventsCount;
        response.defectsCount = defectsCount;
        response.avgDefectRate = avgDefectRate;
        response.status = status;

        return response;
     }
    @Transactional(readOnly = true)
    public List<TopDefectLineResponse> getTopDefectLines(
        Instant from,
        Instant to,
        int limit
    ) {
        List<Object[]> rows = repository.findTopDefectLines(from, to);

        return rows.stream()
            .limit(limit)
            .map(row -> {
                String lineId = (String) row[0];
                long eventCount = (long) row[1];
                long totalDefects = (long) row[2];

                double percent =
                        eventCount > 0
                                ? (totalDefects * 100.0) / eventCount
                                : 0.0;

                TopDefectLineResponse r = new TopDefectLineResponse();
                r.lineId = lineId;
                r.eventCount = eventCount;
                r.totalDefects = totalDefects;
                r.defectsPercent =
                        Math.round(percent * 100.0) / 100.0;

                return r;
            })
            .toList();
    }
}