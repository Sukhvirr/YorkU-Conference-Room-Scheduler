# YorkU Conference Room Scheduler — Deliverable 2

A complete Java 17 Swing implementation of EECS 3311 Group 11's conference-room scheduler. The application has no third-party runtime dependencies and persists its data in CSV files under `data/`.

## Run

From the project directory (the `.bat` launchers also handle restrictive PowerShell execution policies):

```powershell
.\run.bat
```

To compile only or run the automated integration suite:

```powershell
.\build.bat
.\test.bat
```

The project also includes a standard `pom.xml` for IDE/Maven import. The provided scripts are the verified offline path and only require JDK 17.

## Demo accounts

| Role | Email | Password |
|---|---|---|
| Registered user | `student@yorku.ca` | `student123` |
| Administrator | `admin@yorku.ca` | `admin123` |
| Chief Event Coordinator | `chief@yorku.ca` | `chief123` |

The application creates these accounts and three YorkU rooms on first run. Delete the generated CSV files in `data/` to restore the original demo data.

## Six required design patterns

| Pattern | Implementation | Purpose |
|---|---|---|
| Singleton | `DatabaseManager` | One synchronized CSV persistence gateway |
| Factory Method | `AccountFactory` and role-specific creators | Creates registered users, admins, and chief coordinators without exposing constructors |
| Facade | `SchedulerFacade` | One use-case API between Swing and the model/services |
| Strategy | `PaymentStrategy` plus credit, campus-card, and debit strategies | Interchangeable deposit processing and validation |
| Observer | `BookingObserver`, user/admin notification observers | Decoupled notifications for booking lifecycle events |
| State | `BookingState` and five concrete lifecycle states | Enforces valid pending, confirmed, checked-in, cancelled, and completed transitions |

## Requirement coverage

| Requirement area | Implemented behavior |
|---|---|
| Req1 — accounts | Anyone can register; optional York account verification requires `@yorku.ca`; duplicate and invalid emails are rejected |
| Req2 — administrator accounts | Authenticated Chief Event Coordinator creates, activates/deactivates, and resets administrator accounts |
| Req3 — room booking | Users search by date/time, capacity, and location, then book only an available room |
| Req4 — deposits/check-in | A $10 deposit is paid by one of three payment methods; a valid badge check-in applies it to cost; late check-in forfeits it |
| Req5 — room sensors | Each room has a replaceable associated sensor that scans account badges and reports occupancy |
| Req6/Req7 — room administration/details | Admins add/update rooms, enable/disable them, close them for maintenance, and view schedules; IDs are unique and capacity/location are stored |
| Req8 — booking management | Authorized users edit, move, extend, cancel, and view bookings; availability is rechecked |
| Req9 — availability | Overlap detection prevents double booking and filters unavailable/disabled rooms |
| Req10 — payment choice | Credit card, campus card, and debit are selected at runtime through payment strategies |

Notifications are delivered to both the booking owner and administrators for booking confirmations, edits, cancellations, check-ins, and forfeitures. All accounts, rooms, bookings, and notifications survive application restarts through atomic CSV writes.

## Source layout

- `src/main/java/.../App.java` — entry point
- `src/main/java/.../MainFrame.java` — Swing GUI
- `src/main/java/.../SchedulerFacade.java` — use cases and authorization
- `src/main/java/.../DatabaseManager.java` — CSV persistence
- `src/main/java/.../Booking*.java`, `PaymentStrategy.java`, `AccountFactory.java` — pattern implementations
- `src/test/java/.../IntegrationTest.java` — dependency-free end-to-end tests
