package ca.yorku.eecs3311.roomscheduler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Booking {
    public enum DepositStatus { PAID, REFUNDED, APPLIED_TO_COST, FORFEITED }
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
    private final BigDecimal hourlyRate;
    private final BigDecimal depositAmount;
    private final List<BookingObserver> observers = new ArrayList<>();

    public Booking(String id, String accountId, String roomId, LocalDateTime start, LocalDateTime end,
                   int attendees, BookingState state, DepositStatus depositStatus,
                   String paymentMethod, String transactionId,
                   BigDecimal hourlyRate, BigDecimal depositAmount) {
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
        if (hourlyRate == null || hourlyRate.signum() <= 0)
            throw new IllegalArgumentException("Hourly rate must be positive.");
        if (depositAmount == null || depositAmount.signum() <= 0)
            throw new IllegalArgumentException("Deposit must be positive.");
        this.hourlyRate = hourlyRate.setScale(2, RoundingMode.HALF_UP);
        this.depositAmount = depositAmount.setScale(2, RoundingMode.HALF_UP);
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

    public void extendTo(LocalDateTime newEnd) {
        if (!state.name().equals("CONFIRMED") && !state.name().equals("CHECKED_IN"))
            throw new IllegalStateException("Only confirmed or checked-in bookings may be extended.");
        if (newEnd == null || !newEnd.isAfter(end))
            throw new IllegalArgumentException("The new expiry must be after the current expiry.");
        end = newEnd;
        notifyObservers("Booking extended");
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
    public BigDecimal hourlyRate() { return hourlyRate; }
    public BigDecimal depositAmount() { return depositAmount; }
    public BigDecimal estimatedTotal() {
        BigDecimal hours = BigDecimal.valueOf(Duration.between(start, end).toMinutes())
                .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        return hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}
