package ca.yorku.eecs3311.roomscheduler;

public final class RegisteredUser extends Account {
    RegisteredUser(String id, String name, String email, String passwordHash,
                   boolean universityVerified, boolean active) {
        super(id, name, email, passwordHash, Role.REGISTERED_USER, universityVerified, active);
    }
}

