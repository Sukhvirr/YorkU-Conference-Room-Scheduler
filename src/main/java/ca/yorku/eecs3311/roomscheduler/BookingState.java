package ca.yorku.eecs3311.roomscheduler;

/** State pattern: lifecycle rules are delegated to state objects. */
public interface BookingState {
    String name();
    default void confirm(Booking booking) { invalid("confirm"); }
    default void checkIn(Booking booking) { invalid("check in"); }
    default void cancel(Booking booking) { invalid("cancel"); }
    default void complete(Booking booking) { invalid("complete"); }
    default void expire(Booking booking) { invalid("expire"); }
    default boolean editable() { return false; }
    private void invalid(String action) {
        throw new IllegalStateException("Cannot " + action + " a " + name().toLowerCase() + " booking.");
    }

    static BookingState fromName(String name) {
        return switch (name) {
            case "PENDING" -> new PendingState();
            case "CONFIRMED" -> new ConfirmedState();
            case "CHECKED_IN" -> new CheckedInState();
            case "CANCELLED" -> new CancelledState();
            case "COMPLETED" -> new CompletedState();
            default -> throw new IllegalArgumentException("Unknown booking state: " + name);
        };
    }
}

final class PendingState implements BookingState {
    public String name() { return "PENDING"; }
    public void confirm(Booking booking) { booking.transitionTo(new ConfirmedState(), "Booking confirmed"); }
    public void cancel(Booking booking) { booking.transitionTo(new CancelledState(), "Booking cancelled"); }
    public boolean editable() { return true; }
}

final class ConfirmedState implements BookingState {
    public String name() { return "CONFIRMED"; }
    public void checkIn(Booking booking) { booking.transitionTo(new CheckedInState(), "Checked in"); }
    public void cancel(Booking booking) { booking.transitionTo(new CancelledState(), "Booking cancelled"); }
    public void expire(Booking booking) { booking.transitionTo(new CancelledState(), "Booking cancelled as a no-show"); }
    public boolean editable() { return true; }
}

final class CheckedInState implements BookingState {
    public String name() { return "CHECKED_IN"; }
    public void complete(Booking booking) { booking.transitionTo(new CompletedState(), "Booking completed"); }
}

final class CancelledState implements BookingState { public String name() { return "CANCELLED"; } }
final class CompletedState implements BookingState { public String name() { return "COMPLETED"; } }
