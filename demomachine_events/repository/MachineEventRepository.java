package com.buyogo.demomachine_events.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.buyogo.demomachine_events.entity.MachineEvent;

public interface MachineEventRepository extends JpaRepository<MachineEvent, Long> {

    Optional<MachineEvent> findByEventId(String eventId);

    @Query("""
        SELECT e
        FROM MachineEvent e
        WHERE e.machineId = :machineId
          AND e.eventTime >= :start
          AND e.eventTime < :end
    """)
    List<MachineEvent> findEventsForMachineInWindow(
            @Param("machineId") String machineId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT COUNT(e)
        FROM MachineEvent e
        WHERE e.machineId = :machineId
          AND e.eventTime >= :start
          AND e.eventTime < :end
    """)
    long countEventsForMachineInWindow(
            @Param("machineId") String machineId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT COALESCE(SUM(e.defectCount), 0)
        FROM MachineEvent e
        WHERE e.machineId = :machineId
          AND e.eventTime >= :start
          AND e.eventTime < :end
          AND e.defectCount <> -1
    """)
    long sumDefectsForMachineInWindow(
            @Param("machineId") String machineId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
    @Query("""
    SELECT e.machineId,
           COUNT(e),
           SUM(CASE WHEN e.defectCount <> -1 THEN e.defectCount ELSE 0 END)
    FROM MachineEvent e
    WHERE e.eventTime >= :start
      AND e.eventTime < :end
    GROUP BY e.machineId
    ORDER BY SUM(CASE WHEN e.defectCount <> -1 THEN e.defectCount ELSE 0 END) DESC
    """)
    List<Object[]> findTopDefectLines(
        @Param("start") Instant start,
        @Param("end") Instant end
    );

}
