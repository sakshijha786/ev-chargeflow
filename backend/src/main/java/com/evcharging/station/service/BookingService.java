package com.evcharging.station.service;

import com.evcharging.station.dto.BookingRequest;
import com.evcharging.station.dto.WalkInRequest;
import com.evcharging.station.entity.Booking;
import com.evcharging.station.entity.Charger;
import com.evcharging.station.entity.User;
import com.evcharging.station.entity.WaitingQueueEntry;
import com.evcharging.station.enums.BookingStatus;
import com.evcharging.station.enums.ChargerStatus;
import com.evcharging.station.enums.PriorityLevel;
import com.evcharging.station.exception.InvalidBookingStateException;
import com.evcharging.station.exception.ResourceNotFoundException;
import com.evcharging.station.repository.BookingRepository;
import com.evcharging.station.repository.ChargerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ChargerRepository chargerRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final WaitingQueueService waitingQueueService;
    private final InvoiceService invoiceService;

    @Value("${ev.booking.grace-period-minutes:5}")
    private int gracePeriodMinutes;

    /**
     * Dynamic Slot Allocation Engine:
     * - Evaluates incoming requests without manual bay selection.
     * - Matches available chargers by power rating:
     *   * Emergency & Premium -> Fast DC bays (highest powerRatingKw)
     *   * Standard -> Standard bays (lower powerRatingKw), or fastest available if only option
     * - If no charger is available, automatically queues by Priority Level (Emergency > Premium > Standard),
     *   breaking ties strictly with FIFO arrival timestamps.
     */
    @Transactional
    public Object bookCharger(BookingRequest request) {
        User user = null;
        if (request.getUserId() != null) {
            user = userService.getUserById(request.getUserId());
        }

        PriorityLevel priority = request.getPriority() != null ? request.getPriority() : PriorityLevel.STANDARD;
        Double targetKwh = request.getEnergyKwh() != null ? request.getEnergyKwh() : 25.0;

        List<Charger> availableChargers = chargerRepository.findByStatus(ChargerStatus.AVAILABLE);

        if (availableChargers.isEmpty()) {
            WaitingQueueEntry entry = waitingQueueService.addToQueue(
                    user,
                    priority,
                    targetKwh,
                    Boolean.TRUE.equals(request.getIsWalkIn()),
                    request.getCustomerName(),
                    request.getCustomerPhone()
            );
            int position = waitingQueueService.getQueuePosition(entry.getId());
            if (user != null && user.getEmail() != null) {
                emailService.sendQueueJoinedNotification(user.getEmail(), user.getName(), position);
            }
            log.info("No bay available. Enqueued user {} at priority {} (Position #{})",
                    entry.getCustomerName(), priority, position);
            return entry;
        }

        // Match charger by power rating
        Charger selectedCharger = selectBestCharger(availableChargers, priority);

        return createBookingForCharger(
                user,
                selectedCharger,
                priority,
                targetKwh,
                Boolean.TRUE.equals(request.getIsWalkIn()),
                request.getCustomerName(),
                false
        );
    }

    /**
     * Registers a walk-in customer on-site.
     */
    @Transactional
    public Object registerWalkIn(WalkInRequest request) {
        BookingRequest bReq = BookingRequest.builder()
                .userId(null)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .vehicleModel(request.getVehicleModel())
                .energyKwh(request.getEnergyKwh())
                .priority(request.getPriority() != null ? request.getPriority() : PriorityLevel.STANDARD)
                .isWalkIn(true)
                .build();
        return bookCharger(bReq);
    }

    /**
     * Power constraint matching:
     * Priority requests (Emergency / Premium) get the fastest DC charger.
     * Standard requests take lowest available power to preserve high kW bays.
     */
    private Charger selectBestCharger(List<Charger> availableChargers, PriorityLevel priority) {
        if (priority == PriorityLevel.EMERGENCY || priority == PriorityLevel.PREMIUM) {
            return availableChargers.stream()
                    .max(Comparator.comparing(c -> c.getPowerRatingKw() != null ? c.getPowerRatingKw() : 0.0))
                    .orElse(availableChargers.get(0));
        } else {
            return availableChargers.stream()
                    .min(Comparator.comparing(c -> c.getPowerRatingKw() != null ? c.getPowerRatingKw() : 0.0))
                    .orElse(availableChargers.get(0));
        }
    }

    private Booking createBookingForCharger(User user,
                                            Charger charger,
                                            PriorityLevel priority,
                                            Double targetKwh,
                                            boolean isWalkIn,
                                            String customerName,
                                            boolean fromQueue) {
        LocalDateTime now = LocalDateTime.now();

        charger.setStatus(ChargerStatus.RESERVED);
        chargerRepository.save(charger);

        String name = customerName;
        if (name == null && user != null) {
            name = user.getName();
        }

        Booking booking = Booking.builder()
                .user(user)
                .charger(charger)
                .status(BookingStatus.PENDING_ARRIVAL)
                .priority(priority)
                .energyTargetKwh(targetKwh)
                .energyDeliveredKwh(0.0)
                .isWalkIn(isWalkIn)
                .customerName(name != null ? name : "Customer")
                .bookingTime(now)
                .arrivalDeadline(now.plusMinutes(gracePeriodMinutes))
                .reminderSent(false)
                .build();

        booking = bookingRepository.save(booking);

        if (user != null && user.getEmail() != null) {
            if (fromQueue) {
                emailService.sendQueueAllocation(booking);
            } else {
                emailService.sendBookingConfirmation(booking);
            }
        }
        log.info("Charger {} allocated to {} [Priority: {}, Grace Deadline: {}]",
                charger.getCode(), booking.getCustomerName(), priority, booking.getArrivalDeadline());
        return booking;
    }

    /** Driver arrives at the bay within grace period; charging initiates. */
    @Transactional
    public Booking confirmArrival(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING_ARRIVAL) {
            throw new InvalidBookingStateException(
                    "Cannot confirm arrival. Booking is in status: " + booking.getStatus());
        }

        if (LocalDateTime.now().isAfter(booking.getArrivalDeadline())) {
            throw new InvalidBookingStateException(
                    "The arrival grace period has expired for this booking. " +
                    "It has been reallocated to the next queued driver.");
        }

        booking.setStatus(BookingStatus.ACTIVE);
        booking.setArrivedAt(LocalDateTime.now());
        booking.setChargingStartedAt(LocalDateTime.now());

        Charger charger = booking.getCharger();
        charger.setStatus(ChargerStatus.CHARGING);
        chargerRepository.save(charger);

        log.info("Driver arrived at Bay {}. Charging started for Booking #{}", charger.getCode(), booking.getId());
        return bookingRepository.save(booking);
    }

    /** Completes session, records energy delivered, creates invoice, promotes queue. */
    @Transactional
    public Booking completeCharging(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new InvalidBookingStateException(
                    "Cannot complete a booking that is not active. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setChargingEndedAt(LocalDateTime.now());
        if (booking.getEnergyDeliveredKwh() == null || booking.getEnergyDeliveredKwh() <= 0.0) {
            booking.setEnergyDeliveredKwh(booking.getEnergyTargetKwh() != null ? booking.getEnergyTargetKwh() : 25.0);
        }
        bookingRepository.save(booking);

        // Generate Invoice
        invoiceService.generateInvoice(booking);

        // Free charger and auto-promote next queued driver
        releaseChargerAndPromoteQueue(booking.getCharger());
        return booking;
    }

    /** Driver or Operator cancels session; bay freed. */
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING_ARRIVAL && booking.getStatus() != BookingStatus.ACTIVE) {
            throw new InvalidBookingStateException(
                    "Cannot cancel a booking with status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        releaseChargerAndPromoteQueue(booking.getCharger());
        return booking;
    }

    /** Grace period timed out with no arrival: slot passes immediately to next queued driver. */
    @Transactional
    public void expireBooking(Booking booking) {
        booking.setStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        if (booking.getUser() != null && booking.getUser().getEmail() != null) {
            emailService.sendBookingExpired(booking);
        }

        log.warn("Booking #{} expired (no-show). Reallocating Bay {}...", booking.getId(), booking.getCharger().getCode());
        releaseChargerAndPromoteQueue(booking.getCharger());
    }

    /**
     * Bay freed: checks deterministic priority queue. If pending entries exist,
     * immediately allocates the highest priority entry; otherwise marks charger AVAILABLE.
     */
    private void releaseChargerAndPromoteQueue(Charger charger) {
        Optional<WaitingQueueEntry> nextInQueue = waitingQueueService.getNextInQueue();

        if (nextInQueue.isPresent()) {
            WaitingQueueEntry entry = nextInQueue.get();
            waitingQueueService.markAllocated(entry);

            createBookingForCharger(
                    entry.getUser(),
                    charger,
                    entry.getPriority(),
                    entry.getEnergyKwhNeeded(),
                    Boolean.TRUE.equals(entry.getIsWalkIn()),
                    entry.getCustomerName(),
                    true
            );
            log.info("Charger {} auto-reallocated to queued driver {} (Priority: {})",
                    charger.getCode(), entry.getCustomerName(), entry.getPriority());
        } else {
            charger.setStatus(ChargerStatus.AVAILABLE);
            chargerRepository.save(charger);
            log.info("Charger {} is now AVAILABLE", charger.getCode());
        }
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}
