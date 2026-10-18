package com.evcharging.station.controller;

import com.evcharging.station.dto.ApiResponse;
import com.evcharging.station.dto.StationMetricsResponse;
import com.evcharging.station.entity.Charger;
import com.evcharging.station.enums.BookingStatus;
import com.evcharging.station.enums.ChargerStatus;
import com.evcharging.station.service.BookingService;
import com.evcharging.station.service.ChargerService;
import com.evcharging.station.service.InvoiceService;
import com.evcharging.station.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ChargerService chargerService;
    private final BookingService bookingService;
    private final WaitingQueueService waitingQueueService;
    private final InvoiceService invoiceService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<StationMetricsResponse>> getMetrics() {
        List<Charger> chargers = chargerService.getAllChargers();
        long total = chargers.size();
        long available = chargers.stream().filter(c -> c.getStatus() == ChargerStatus.AVAILABLE).count();
        long outOfService = chargers.stream().filter(c -> c.getStatus() == ChargerStatus.OUT_OF_SERVICE).count();
        long active = bookingService.getBookingsByStatus(BookingStatus.ACTIVE).size();
        long pending = bookingService.getBookingsByStatus(BookingStatus.PENDING_ARRIVAL).size();
        long completed = bookingService.getBookingsByStatus(BookingStatus.COMPLETED).size();
        long queueDepth = waitingQueueService.getWaitingQueue().size();

        StationMetricsResponse metrics = StationMetricsResponse.builder()
                .totalChargers(total)
                .availableChargers(available)
                .activeChargingSessions(active)
                .pendingArrivals(pending)
                .outOfServiceChargers(outOfService)
                .queueDepth(queueDepth)
                .totalRevenueInr(invoiceService.getTotalRevenue())
                .totalEnergyDeliveredKwh(invoiceService.getTotalEnergyDelivered())
                .totalGstCollectedInr(invoiceService.getTotalGst())
                .completedSessions(completed)
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Station metrics fetched", metrics));
    }
}
