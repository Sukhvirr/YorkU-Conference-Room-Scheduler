package ca.yorku.eecs3311.roomscheduler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Booking {
    public enum DepositStatus { PAID, REFUNDED, APPLIED_TO_COST, FORFEITED }
    public static final BigDecimal DEPOSIT = new BigDecimal("10.00");

    private final String id;
    private final String accountId;
    private String roomId;
    private LocalDateTime start;
    private LocalDateTime end;
    private int attendees;
    private BookingState state;
    private DepositStatus depositStatus;
    private final String paymentMethod;
    private final String transactionId;
    private final List<BookingObserver> observers = new ArrayList<>();

    public Booking(String id, String accountId, String roomId, LocalDateTime start, LocalDateTime end,
                   int attendees, BookingState state, DepositStatus depositStatus,
                   String paymentMethod, String transactionId) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.roomId = Objects.requireNonNull(roomId);
        validateTimes(start, end);
        if (attendees < 1) throw new IllegalArgumentException("Attendees must be positive.");
        this.start = start;
        this.end = end;
        this.attendees = attendees;
        this.state = Objects.requireNonNull(state);
        this.depositStatus = Objects.requireNonNull(depositStatus);
        this.paymentMethod = Objects.requireNonNull(paymentMethod);
        this.transactionId = Objects.requireNonNull(transactionId);
    }

    private static void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start))
            throw new IllegalArgumentException("End time must be after start time.");
    }

    public void attach(BookingObserver observer) { observers.add(observer); }
    public void confirm() { state.confirm(this); }
    public void checkIn() { state.checkIn(this); depositStatus = DepositStatus.APPLIED_TO_COST; }
    public void cancel() { state.cancel(this); depositStatus = DepositStatus.REFUNDED; }
    public void complete() { state.complete(this); }
    public void forfeitDeposit() { depositStatus = DepositStatus.FORFEITED; notifyObservers("Deposit forfeited"); }
    public void markNoShow() {
        depositStatus = DepositStatus.FORFEITED;
        state.expire(this);
    }

    void transitionTo(BookingState next, String event) {
        state = next;
        notifyObservers(event);
    }

    public void reschedule(String roomId, LocalDateTime start, LocalDateTime end, int attendees) {
        if (!state.editable()) throw new IllegalStateException("This booking can no longer be edited.");
        validateTimes(start, end);
        if (attendees < 1) throw new IllegalArgumentException("Attendees must be positive.");
        this.roomId = Objects.requireNonNull(roomId);
        this.start = start;
        this.end = end;
        this.attendees = attendees;
        notifyObservers("Booking updated");
    }

    private void notifyObservers(String event) {
        List.copyOf(observers).forEach(observer -> observer.onBookingChanged(this, event));
    }

    public boolean overlaps(LocalDateTime candidateStart, LocalDateTime candidateEnd) {
        return !state.name().equals("CANCELLED") && candidateStart.isBefore(end) && candidateEnd.isAfter(start);
    }

    public String id() { return id; }
    public String accountId() { return accountId; }
    public String roomId() { return roomId; }
    public LocalDateTime start() { return start; }
    public LocalDateTime end() { return end; }
    public int attendees() { return attendees; }
    public BookingState state() { return state; }
    public DepositStatus depositStatus() { return depositStatus; }
    public String paymentMethod() { return paymentMethod; }
    public String transactionId() { return transactionId; }
}
