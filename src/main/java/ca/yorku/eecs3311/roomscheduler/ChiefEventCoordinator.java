package ca.yorku.eecs3311.roomscheduler;

public final class ChiefEventCoordinator extends Account {
    ChiefEventCoordinator(String id, String name, String email, String passwordHash, boolean active) {
        super(id, name, email, passwordHash, Role.CHIEF_EVENT_COORDINATOR, null, "", true, active);
    }
}
