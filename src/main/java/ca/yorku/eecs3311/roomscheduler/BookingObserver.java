package ca.yorku.eecs3311.roomscheduler;

import java.time.LocalDateTime;

/** Observer pattern: booking events notify user/admin observers without GUI coupling. */
public interface BookingObserver {
    void onBookingChanged(Booking booking, String event);
}

record Notification(String id, String accountId, LocalDateTime createdAt, String message) {}

final class AccountNotificationObserver implements BookingObserver {
    private final SchedulerFacade scheduler;
    private final String accountId;

    AccountNotificationObserver(SchedulerFacade scheduler, String accountId) {
        this.scheduler = scheduler;
        this.accountId = accountId;
    }

    @Override public void onBookingChanged(Booking booking, String event) {
        scheduler.recordNotification(accountId, event + ": " + booking.id() + " in room " + booking.roomId());
    }
}

final class AdministratorNotificationObserver implements BookingObserver {
    private final SchedulerFacade scheduler;
    AdministratorNotificationObserver(SchedulerFacade scheduler) { this.scheduler = scheduler; }
    @Override public void onBookingChanged(Booking booking, String event) {
        scheduler.notifyAdministrators(event + ": " + booking.id() + " in room " + booking.roomId());
    }
}

