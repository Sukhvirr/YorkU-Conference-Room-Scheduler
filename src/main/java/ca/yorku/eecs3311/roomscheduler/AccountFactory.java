package ca.yorku.eecs3311.roomscheduler;

import java.util.UUID;

/** Factory Method pattern: callers request a role, not a concrete account constructor. */
public abstract class AccountFactory {
    public final Account createAccount(String name, String email, String plainPassword,
                                       boolean universityVerified) {
        return create(UUID.randomUUID().toString(), name, email,
                Account.hashPassword(plainPassword), universityVerified, true);
    }

    protected abstract Account create(String id, String name, String email, String passwordHash,
                                      boolean universityVerified, boolean active);

    public static AccountFactory forRole(Role role) {
        return switch (role) {
            case REGISTERED_USER -> new RegisteredUserFactory();
            case ADMIN -> new AdminFactory();
            case CHIEF_EVENT_COORDINATOR -> new ChiefCoordinatorFactory();
        };
    }

    public static Account restore(String id, String name, String email, String passwordHash,
                                  Role role, boolean universityVerified, boolean active) {
        return forRole(role).create(id, name, email, passwordHash, universityVerified, active);
    }

    private static final class RegisteredUserFactory extends AccountFactory {
        @Override protected Account create(String id, String name, String email, String hash,
                                           boolean verified, boolean active) {
            return new RegisteredUser(id, name, email, hash, verified, active);
        }
    }

    private static final class AdminFactory extends AccountFactory {
        @Override protected Account create(String id, String name, String email, String hash,
                                           boolean verified, boolean active) {
            return new Admin(id, name, email, hash, active);
        }
    }

    private static final class ChiefCoordinatorFactory extends AccountFactory {
        @Override protected Account create(String id, String name, String email, String hash,
                                           boolean verified, boolean active) {
            return new ChiefEventCoordinator(id, name, email, hash, active);
        }
    }
}

