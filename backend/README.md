# EV Charging Station Backend

A Spring Boot REST API for a charging station with multiple chargers: real-time
availability, bookings, email confirmations/reminders, a 5-minute arrival grace
period, and automatic reassignment to a FIFO waiting queue on no-show.

## Stack
- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation, Mail)
- MySQL
- Maven
- `@Scheduled` background jobs (no external queue/broker needed)

## How the core flow works

1. **Book** (`POST /api/bookings`) — the first `AVAILABLE` charger is reserved
   for the user (row-locked to avoid two requests grabbing the same charger).
   Status becomes `PENDING_ARRIVAL`, `arrivalDeadline = now + 5 minutes`, and a
   confirmation email is sent. If no charger is free, the user is placed on the
   waiting queue instead and emailed their queue position.
2. **Reminder** — every 30s (configurable) `ReminderScheduler` emails anyone
   within 2 minutes of their deadline who hasn't been reminded yet.
3. **Arrive** (`POST /api/bookings/{id}/arrive`) — within the grace period,
   this flips the booking to `ACTIVE` and the charger to `CHARGING`.
4. **No-show** — every 30s (configurable) `BookingExpiryScheduler` finds
   `PENDING_ARRIVAL` bookings past their deadline, marks them `EXPIRED`, emails
   the user, and frees the charger.
5. **Queue promotion** — whenever a charger is freed (no-show, cancel, or
   charging completed), the service checks the waiting queue. If someone is
   waiting, they're immediately given a new booking on that charger (with
   their own fresh 5-minute deadline) and emailed; otherwise the charger goes
   back to `AVAILABLE`.

```
BOOK ──► PENDING_ARRIVAL ──arrive──► ACTIVE ──complete──► COMPLETED
             │                                                │
             │ (5 min elapse, no arrival)                     │
             ▼                                                ▼
          EXPIRED  ─────────────────────────────────► charger freed
             │                                                │
             └────────────────► next in WAITING queue ◄───────┘
                                  gets a new booking
```

## Project layout
```
src/main/java/com/evcharging/station/
├── entity/        User, Charger, Booking, WaitingQueueEntry
├── enums/         ChargerStatus, BookingStatus, QueueStatus
├── repository/    Spring Data JPA repositories
├── dto/           Request/response payloads + ApiResponse<T> wrapper
├── service/       ChargerService, UserService, BookingService,
│                  WaitingQueueService, EmailService
├── scheduler/     BookingExpiryScheduler, ReminderScheduler
├── controller/    ChargerController, UserController, BookingController,
│                  QueueController
└── exception/     ResourceNotFoundException, InvalidBookingStateException,
                   GlobalExceptionHandler
```

## Setup

1. **MySQL** — create nothing manually; `createDatabaseIfNotExist=true` and
   `ddl-auto=update` in `application.properties` will create the database and
   tables on first run. Just make sure a MySQL server is reachable and edit:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/ev_charging_db?...
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```
2. **Email** — for Gmail, create an [App Password](https://myaccount.google.com/apppasswords)
   (regular password won't work with 2FA) and set:
   ```properties
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-16-char-app-password
   ```
   Any SMTP provider works — just change `spring.mail.host`/`port`.
3. **Run**:
   ```bash
   mvn spring-boot:run
   ```
   The API starts on `http://localhost:8080`.

## Configuration knobs (`application.properties`)
| Property | Meaning | Default |
|---|---|---|
| `ev.booking.grace-period-minutes` | Arrival window before auto-cancel | 5 |
| `ev.booking.reminder-before-minutes` | Reminder sent this many minutes before deadline | 2 |
| `ev.scheduler.expiry-check-rate-ms` | How often the no-show sweep runs | 30000 |
| `ev.scheduler.reminder-check-rate-ms` | How often the reminder sweep runs | 30000 |

## API reference

### Users
| Method | Path | Body |
|---|---|---|
| POST | `/api/users` | `{"name": "Asha Rao", "email": "asha@example.com", "phone": "9999999999"}` |
| GET | `/api/users/{id}` | — |
| GET | `/api/users` | — |

### Chargers
| Method | Path | Notes |
|---|---|---|
| POST | `/api/chargers` | `{"code": "CHG-01", "location": "Level 1 - Bay A", "powerRatingKw": 50}` |
| GET | `/api/chargers` | Real-time status of every charger |
| GET | `/api/chargers/available` | Real-time list of free chargers |
| GET | `/api/chargers/{id}` | Single charger |
| PATCH | `/api/chargers/{id}/status?status=OUT_OF_SERVICE` | Manual override |

### Bookings
| Method | Path | Notes |
|---|---|---|
| POST | `/api/bookings` | `{"userId": 1}` — books, or queues if nothing free |
| POST | `/api/bookings/{id}/arrive` | Confirms arrival within grace period |
| POST | `/api/bookings/{id}/complete` | Ends the charging session |
| POST | `/api/bookings/{id}/cancel` | User-initiated cancel |
| GET | `/api/bookings/{id}` | Single booking |
| GET | `/api/bookings` | All bookings |

### Waiting queue
| Method | Path |
|---|---|
| GET | `/api/queue` | Users currently waiting, in order |

## Try it end-to-end (curl)

```bash
# 1. Register a user
curl -s -X POST localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Asha Rao","email":"asha@example.com"}'

# 2. Add a charger
curl -s -X POST localhost:8080/api/chargers \
  -H "Content-Type: application/json" \
  -d '{"code":"CHG-01","location":"Bay A","powerRatingKw":50}'

# 3. Check real-time availability
curl -s localhost:8080/api/chargers/available

# 4. Book it (userId=1)
curl -s -X POST localhost:8080/api/bookings \
  -H "Content-Type: application/json" -d '{"userId":1}'
# -> confirmation email sent, arrivalDeadline = now + 5 min

# 5a. Arrive in time
curl -s -X POST localhost:8080/api/bookings/1/arrive

# 5b. OR do nothing — after 5 minutes the scheduler auto-expires the booking
#     and, if someone else booked while all chargers were full, reassigns
#     the charger to them automatically (check /api/queue and /api/bookings)
```

## Design notes
- **Waiting queue is station-wide, not per-charger**: since any charger can
  serve any user, a single FIFO queue is simpler and gets everyone charged
  faster than per-charger queues.
- **Pessimistic locking** (`ChargerRepository.findFirstByStatus`) prevents two
  simultaneous booking requests from both reserving the same charger.
- **Email failures never break booking logic** — `EmailService` catches and
  logs SMTP errors so a mail outage can't leave chargers stuck in `RESERVED`.
- Entities are returned directly by some endpoints (with
  `spring.jpa.open-in-view=true`) for simplicity; for a production API you'd
  likely replace these with DTOs everywhere, as already done for bookings.
