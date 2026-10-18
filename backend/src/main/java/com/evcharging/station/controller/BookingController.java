package com.evcharging.station.controller;

import com.evcharging.station.dto.ApiResponse;
import com.evcharging.station.dto.BookingRequest;
import com.evcharging.station.dto.BookingResponse;
import com.evcharging.station.dto.WalkInRequest;
import com.evcharging.station.entity.Booking;
import com.evcharging.station.entity.WaitingQueueEntry;
import com.evcharging.station.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Dynamic Slot Allocation:
     * Assigns the optimal charger according to power constraints and priority,
     * or queues the user deterministically (Emergency > Premium > Standard, tie-broken by FIFO).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> bookCharger(@Valid @RequestBody BookingRequest request) {
        Object result = bookingService.bookCharger(request);

        if (result instanceof Booking booking) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Charger allocated. Arrive within grace period.", BookingResponse.fromEntity(booking)));
        }

        WaitingQueueEntry entry = (WaitingQueueEntry) result;
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("All bays currently occupied. Added to deterministic priority queue.", entry));
    }

    /**
     * Operator registers on-site walk-in customer.
     */
    @PostMapping("/walkin")
    public ResponseEntity<ApiResponse<Object>> registerWalkIn(@Valid @RequestBody WalkInRequest request) {
        Object result = bookingService.registerWalkIn(request);

        if (result instanceof Booking booking) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Walk-in customer assigned to Bay " + booking.getCharger().getCode(), BookingResponse.fromEntity(booking)));
        }

        WaitingQueueEntry entry = (WaitingQueueEntry) result;
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("All bays occupied. Walk-in customer added to priority queue.", entry));
    }

    @PostMapping("/{id}/arrive")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmArrival(@PathVariable Long id) {
        Booking booking = bookingService.confirmArrival(id);
        return ResponseEntity.ok(ApiResponse.ok("Arrival confirmed. Charging started.", BookingResponse.fromEntity(booking)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> completeCharging(@PathVariable Long id) {
        Booking booking = bookingService.completeCharging(id);
        return ResponseEntity.ok(ApiResponse.ok("Charging completed and invoice generated.", BookingResponse.fromEntity(booking)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {
        Booking booking = bookingService.cancelBooking(id);
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled. Bay released.", BookingResponse.fromEntity(booking)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.ok("Booking fetched", BookingResponse.fromEntity(booking)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.ok("Bookings fetched", bookingService.getAllBookings()));
    }
}
