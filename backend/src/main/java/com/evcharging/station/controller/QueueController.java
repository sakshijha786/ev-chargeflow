package com.evcharging.station.controller;

import com.evcharging.station.dto.ApiResponse;
import com.evcharging.station.entity.WaitingQueueEntry;
import com.evcharging.station.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final WaitingQueueService waitingQueueService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WaitingQueueEntry>>> getWaitingQueue() {
        return ResponseEntity.ok(ApiResponse.ok("Waiting queue fetched", waitingQueueService.getWaitingQueue()));
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<ApiResponse<WaitingQueueEntry>> promoteQueueEntry(@PathVariable Long id) {
        WaitingQueueEntry updated = waitingQueueService.promotePriority(id);
        return ResponseEntity.ok(ApiResponse.ok("Queue priority elevated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromQueue(@PathVariable Long id) {
        waitingQueueService.removeFromQueue(id);
        return ResponseEntity.ok(ApiResponse.ok("Removed from queue", null));
    }
}
