package com.buyogo.demomachine_events;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.buyogo.demomachine_events.dto.BatchIngestResponse;
import com.buyogo.demomachine_events.dto.EventIngestRequest;
import com.buyogo.demomachine_events.entity.MachineEvent;
import com.buyogo.demomachine_events.repository.MachineEventRepository;
import com.buyogo.demomachine_events.service.EventIngestService;

@SpringBootTest
@Transactional
class EventIngestServiceTest {

    @Autowired
    private EventIngestService ingestService;

    @Autowired
    private MachineEventRepository repository;

    @BeforeEach
    void clearDb() {
        repository.deleteAll();
    }

    private EventIngestRequest event(
            String id,
            Instant time,
            long duration,
            int defects
        ) {
        EventIngestRequest e = new EventIngestRequest();
        e.eventId = id;
        e.eventTime = time;
        e.machineId = "M-001";
        e.durationMs = duration;
        e.defectCount = defects;
        return e;
    }
    @Test
    void identicalDuplicateEventIsDeduped() {
        Instant t = Instant.now();

        EventIngestRequest e1 = event("E-1", t, 1000, 0);
        EventIngestRequest e2 = event("E-1", t, 1000, 0);

        BatchIngestResponse r1 = ingestService.ingestBatch(List.of(e1));
        BatchIngestResponse r2 = ingestService.ingestBatch(List.of(e2));

        assertEquals(1, r1.accepted);
        assertEquals(1, r2.deduped);
        assertEquals(1, repository.count());
    }
    @Test
    void newerPayloadUpdatesExistingEvent() {
        Instant t = Instant.now();

        ingestService.ingestBatch(
                List.of(event("E-2", t, 1000, 0))
        );

        BatchIngestResponse r =
                ingestService.ingestBatch(
                        List.of(event("E-2", t, 1000, 5))
                );

        MachineEvent stored =
                repository.findByEventId("E-2").orElseThrow();

        assertEquals(1, r.updated);
        assertEquals(5, stored.getDefectCount());
    }
        @Test
    void invalidDurationIsRejected() {
        EventIngestRequest e =
                event("E-3", Instant.now(), -10, 0);

        BatchIngestResponse r =
                ingestService.ingestBatch(List.of(e));

        assertEquals(1, r.rejected);
        assertEquals(0, repository.count());
    }

        @Test
    void futureEventTimeIsRejected() {
        Instant future =
                Instant.now().plusSeconds(3600);

        EventIngestRequest e =
                event("E-4", future, 1000, 0);

        BatchIngestResponse r =
                ingestService.ingestBatch(List.of(e));

        assertEquals(1, r.rejected);
    }

        @Test
    void defectMinusOneIgnoredInStats() {
        Instant t = Instant.now();

        ingestService.ingestBatch(
                List.of(
                        event("E-5", t, 1000, -1),
                        event("E-6", t, 1000, 3)
                )
        );

        long defects =
                repository.sumDefectsForMachineInWindow(
                        "M-001",
                        t.minusSeconds(10),
                        t.plusSeconds(10)
                );

        assertEquals(3, defects);
    }

        @Test
    void startInclusiveEndExclusive() {
        Instant t = Instant.parse("2026-01-15T04:30:00Z");

        ingestService.ingestBatch(
                List.of(event("E-7", t, 1000, 0))
        );

        long count =
                repository.countEventsForMachineInWindow(
                        "M-001",
                        t,
                        t.plusSeconds(1)
                );

        assertEquals(1, count);

        long zero =
                repository.countEventsForMachineInWindow(
                        "M-001",
                        t.plusSeconds(1),
                        t.plusSeconds(10)
                );

        assertEquals(0, zero);
    }

        @Test
    void concurrentIngestionIsThreadSafe() throws Exception {
        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        Instant t = Instant.now();

        for (int i = 0; i < 20; i++) {
            executor.submit(() ->
                    ingestService.ingestBatch(
                            List.of(event("E-100", t, 1000, 1))
                    )
            );
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, repository.count());
    }
}
