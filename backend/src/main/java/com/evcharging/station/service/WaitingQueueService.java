package com.evcharging.station.service;

import com.evcharging.station.entity.User;
import com.evcharging.station.entity.WaitingQueueEntry;
import com.evcharging.station.enums.PriorityLevel;
import com.evcharging.station.enums.QueueStatus;
import com.evcharging.station.exception.ResourceNotFoundException;
import com.evcharging.station.repository.WaitingQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingQueueService {

    private final WaitingQueueRepository waitingQueueRepository;

    public WaitingQueueEntry addToQueue(User user) {
        return addToQueue(user, PriorityLevel.STANDARD, 25.0, false, user != null ? user.getName() : null, user != null ? user.getPhone() : null);
    }

    public WaitingQueueEntry addToQueue(User user, PriorityLevel priority, Double energyKwh, Boolean isWalkIn, String customerName, String customerPhone) {
        WaitingQueueEntry entry = WaitingQueueEntry.builder()
                .user(user)
                .priority(priority != null ? priority : PriorityLevel.STANDARD)
                .energyKwhNeeded(energyKwh != null ? energyKwh : 25.0)
                .isWalkIn(Boolean.TRUE.equals(isWalkIn))
                .customerName(customerName != null ? customerName : (user != null ? user.getName() : "Walk-in Guest"))
                .customerPhone(customerPhone != null ? customerPhone : (user != null ? user.getPhone() : null))
                .status(QueueStatus.WAITING)
                .build();
        return waitingQueueRepository.save(entry);
    }

    /**
     * Deterministic Queue:
     * 1. Priority: Emergency (3) > Premium (2) > Standard (1)
     * 2. FIFO: Tie-broken strictly by requestedAt ASC
     */
    public Optional<WaitingQueueEntry> getNextInQueue() {
        return waitingQueueRepository.findNextInQueue(QueueStatus.WAITING);
    }

    public List<WaitingQueueEntry> getWaitingQueue() {
        return waitingQueueRepository.findWaitingQueueOrdered(QueueStatus.WAITING);
    }

    public int getQueuePosition(Long entryId) {
        List<WaitingQueueEntry> waiting = getWaitingQueue();
        for (int i = 0; i < waiting.size(); i++) {
            if (waiting.get(i).getId().equals(entryId)) {
                return i + 1;
            }
        }
        return -1;
    }

    @Transactional
    public void markAllocated(WaitingQueueEntry entry) {
        entry.setStatus(QueueStatus.ALLOCATED);
        entry.setAllocatedAt(LocalDateTime.now());
        waitingQueueRepository.save(entry);
    }

    /**
     * Operator manually promotes an entry's priority.
     */
    @Transactional
    public WaitingQueueEntry promotePriority(Long entryId) {
        WaitingQueueEntry entry = waitingQueueRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with id: " + entryId));
        if (entry.getPriority() == PriorityLevel.STANDARD) {
            entry.setPriority(PriorityLevel.PREMIUM);
        } else if (entry.getPriority() == PriorityLevel.PREMIUM) {
            entry.setPriority(PriorityLevel.EMERGENCY);
        }
        log.info("Queue entry {} priority elevated to {}", entryId, entry.getPriority());
        return waitingQueueRepository.save(entry);
    }

    /**
     * Operator removes an entry from the queue.
     */
    @Transactional
    public void removeFromQueue(Long entryId) {
        WaitingQueueEntry entry = waitingQueueRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found with id: " + entryId));
        entry.setStatus(QueueStatus.CANCELLED);
        waitingQueueRepository.save(entry);
        log.info("Queue entry {} removed by operator", entryId);
    }
}
