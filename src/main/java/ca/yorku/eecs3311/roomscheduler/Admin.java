package ca.yorku.eecs3311.roomscheduler;

public final class Admin extends Account {
    Admin(String id, String name, String email, String passwordHash, boolean active) {
        super(id, name, email, passwordHash, Role.ADMIN, true, active);
    }
}

