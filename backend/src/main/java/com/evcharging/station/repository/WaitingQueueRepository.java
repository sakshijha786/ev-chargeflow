package com.evcharging.station.repository;

import com.evcharging.station.entity.WaitingQueueEntry;
import com.evcharging.station.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WaitingQueueRepository extends JpaRepository<WaitingQueueEntry, Long> {

    /**
     * Orders waiting queue entries strictly by:
     * 1. Priority (EMERGENCY > PREMIUM > STANDARD)
     * 2. FIFO (requestedAt ASC) for ties.
     */
    @Query("SELECT q FROM WaitingQueueEntry q WHERE q.status = :status " +
           "ORDER BY CASE q.priority " +
           "  WHEN com.evcharging.station.enums.PriorityLevel.EMERGENCY THEN 3 " +
           "  WHEN com.evcharging.station.enums.PriorityLevel.PREMIUM THEN 2 " +
           "  ELSE 1 END DESC, q.requestedAt ASC")
    List<WaitingQueueEntry> findWaitingQueueOrdered(@Param("status") QueueStatus status);

    @Query("SELECT q FROM WaitingQueueEntry q WHERE q.status = :status " +
           "ORDER BY CASE q.priority " +
           "  WHEN com.evcharging.station.enums.PriorityLevel.EMERGENCY THEN 3 " +
           "  WHEN com.evcharging.station.enums.PriorityLevel.PREMIUM THEN 2 " +
           "  ELSE 1 END DESC, q.requestedAt ASC LIMIT 1")
    Optional<WaitingQueueEntry> findNextInQueue(@Param("status") QueueStatus status);

    List<WaitingQueueEntry> findByStatusOrderByRequestedAtAsc(QueueStatus status);
}
