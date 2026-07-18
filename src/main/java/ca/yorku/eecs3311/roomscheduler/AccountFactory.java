package ca.yorku.eecs3311.roomscheduler;

import java.util.UUID;

/** Factory Method pattern: callers request a role, not a concrete account constructor. */
public abstract class AccountFactory {
    public final Account createAccount(String name, String email, String plainPassword,
                                       UserType userType, String organizationId,
                                       boolean universityVerified) {
        return create(UUID.randomUUID().toString(), name, email,
                Account.hashPassword(plainPassword), userType, organizationId,
                universityVerified, true);
    }

    public final Account createAccount(String name, String email, String plainPassword,
                                       boolean universityVerified) {
        return createAccount(name, email, plainPassword, null, "", universityVerified);
    }

    protected abstract Account create(String id, String name, String email, String passwordHash,
                                      UserType userType, String organizationId,
                                      boolean universityVerified, boolean active);

    public static AccountFactory forRole(Role role) {
        return switch (role) {
            case REGISTERED_USER -> new RegisteredUserFactory();
            case ADMIN -> new AdminFactory();
            case CHIEF_EVENT_COORDINATOR -> new ChiefCoordinatorFactory();
        };
    }

    public static Account restore(String id, String name, String email, String passwordHash,
                                  Role role, UserType userType, String organizationId,
                                  boolean universityVerified, boolean active) {
        return forRole(role).create(id, name, email, passwordHash, userType, organizationId,
                universityVerified, active);
    }

    private static final class RegisteredUserFactory extends AccountFactory {
        @Override protected Account create(String id, String name, String email, String hash,
                                           UserType userType, String organizationId,
                                           boolean verified, boolean active) {
            return new RegisteredUser(id, name, email, hash,
                    userType == null ? UserType.STUDENT : userType,
                    organizationId, verified, active);
        }
    }

    private static final class AdminFactory extends AccountFactory {
        @Override protected Account create(String id, String name, String email, String hash,
                                           UserType userType, String organizationId,
                                           boolean verified, boolean active) {
            return new Admin(id, name, email, hash, active);
        }
    }

    private static final class ChiefCoordinatorFactory extends AccountFactory {
        @Override protected Account create(String id, String name, String email, String hash,
                                           UserType userType, String organizationId,
                                           boolean verified, boolean active) {
            return new ChiefEventCoordinator(id, name, email, hash, active);
        }
    }
}
