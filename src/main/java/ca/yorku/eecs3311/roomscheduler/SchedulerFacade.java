package ca.yorku.eecs3311.roomscheduler;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.security.SecureRandom;

/** Facade pattern: the GUI uses this single API for all application use cases. */
public final class SchedulerFacade {
    private static SchedulerFacade instance;
    private final DatabaseManager database;
    private final List<Account> accounts;
    private final List<Room> rooms;
    private final List<Booking> bookings;
    private final List<Notification> notifications;

    private SchedulerFacade() {
        database = DatabaseManager.getInstance();
        accounts = new ArrayList<>(database.loadAccounts());
        rooms = new ArrayList<>(database.loadRooms());
        bookings = new ArrayList<>(database.loadBookings());
        notifications = new ArrayList<>(database.loadNotifications());
        seedDemoData();
        bookings.forEach(this::attachObservers);
    }

    public static synchronized SchedulerFacade getInstance() {
        if (instance == null) instance = new SchedulerFacade();
        return instance;
    }

    static synchronized void resetForTests() { instance = null; DatabaseManager.resetForTests(); }

    private void seedDemoData() {
        if (accounts.isEmpty()) {
            accounts.add(AccountFactory.forRole(Role.CHIEF_EVENT_COORDINATOR)
                    .createAccount("Chief Coordinator", "chief@yorku.ca", "Chief123!", true));
            accounts.add(AccountFactory.forRole(Role.ADMIN)
                    .createAccount("Room Administrator", "admin@yorku.ca", "Admin123!", true));
            accounts.add(AccountFactory.forRole(Role.REGISTERED_USER)
                    .createAccount("Demo Student", "student@yorku.ca", "Student123!",
                            UserType.STUDENT, "218882191", true));
            database.saveAccounts(accounts);
        } else {
            migrateLegacyDemoPasswords();
        }
        if (rooms.isEmpty()) {
            rooms.add(new Room("LAS-1001", "Lassonde Boardroom", "Lassonde Building", 12, Room.Status.AVAILABLE));
            rooms.add(new Room("ACE-201", "Innovation Room", "Accolade East", 24, Room.Status.AVAILABLE));
            rooms.add(new Room("DB-001", "Executive Conference Room", "Dahdaleh Building", 8, Room.Status.AVAILABLE));
            database.saveRooms(rooms);
        }
    }

    private void migrateLegacyDemoPasswords() {
        boolean changed = false;
        for (Account account : accounts) {
            if (account.email().equals("student@yorku.ca") && account.passwordMatches("student123")) {
                account.setPassword("Student123!");
                changed = true;
            } else if (account.email().equals("admin@yorku.ca") && account.passwordMatches("admin123")) {
                account.setPassword("Admin123!");
                changed = true;
            } else if (account.email().equals("chief@yorku.ca") && account.passwordMatches("chief123")) {
                account.setPassword("Chief123!");
                changed = true;
            }
        }
        if (changed) {
            try {
                database.saveAccounts(accounts);
            } catch (IllegalStateException lockedLegacyFile) {
                // The upgraded credentials still work for this session. A later restart will
                // persist them once accounts.csv is no longer open in Excel or another editor.
            }
        }
    }

    public synchronized Account register(String name, String email, String password,
                                         UserType userType, String organizationId) {
        validateEmail(email);
        if (userType == null) throw new IllegalArgumentException("Select an account type.");
        validateOrganizationId(userType, organizationId);
        if (findAccountByEmail(email) != null) throw new IllegalArgumentException("An account already uses this email.");
        boolean yorkEmail = email.toLowerCase(Locale.ROOT).endsWith("@yorku.ca");
        if (userType.universityType() && !yorkEmail)
            throw new IllegalArgumentException(userType.displayName() + " accounts require a verified @yorku.ca email.");
        boolean verified = yorkEmail; // simulated university verification service
        Account account = AccountFactory.forRole(Role.REGISTERED_USER)
                .createAccount(name, email, password, userType, organizationId, verified);
        accounts.add(account);
        database.saveAccounts(accounts);
        recordNotification(account.id(), verified ? "Account created and York University identity verified."
                : "External partner account created.");
        return account;
    }

    public synchronized Account register(String name, String email, String password, boolean universityAccount) {
        return register(name, email, password, universityAccount ? UserType.STUDENT : UserType.PARTNER,
                universityAccount ? "000000000" : "PARTNER-DEFAULT");
    }

    public synchronized Account authenticate(String email, String password) {
        Account account = findAccountByEmail(email);
        if (account == null || !account.passwordMatches(password))
            throw new IllegalArgumentException("Incorrect email or password.");
        if (!account.active()) throw new IllegalStateException("This account is deactivated.");
        return account;
    }

    public synchronized Account createAdministrator(Account actor, String name, String email, String password) {
        requireRole(actor, Role.CHIEF_EVENT_COORDINATOR);
        validateEmail(email);
        if (findAccountByEmail(email) != null) throw new IllegalArgumentException("Email is already registered.");
        Account admin = AccountFactory.forRole(Role.ADMIN).createAccount(name, email, password, true);
        accounts.add(admin);
        database.saveAccounts(accounts);
        recordNotification(admin.id(), "Administrator credentials created by " + actor.name() + ".");
        return admin;
    }

    public synchronized GeneratedAdminCredentials generateAdministrator(Account actor, String name) {
        requireRole(actor, Role.CHIEF_EVENT_COORDINATOR);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Administrator name is required.");
        String token = UUID.randomUUID().toString().substring(0, 8);
        String email = "admin." + token + "@yorku.ca";
        String password = "Adm!" + token.substring(0, 4).toUpperCase(Locale.ROOT)
                + new SecureRandom().nextInt(1000, 9999);
        Account admin = AccountFactory.forRole(Role.ADMIN).createAccount(name, email, password, true);
        accounts.add(admin);
        database.saveAccounts(accounts);
        recordNotification(admin.id(), "Administrator credentials auto-generated by " + actor.name() + ".");
        return new GeneratedAdminCredentials(admin, password);
    }

    public synchronized void setAdministratorActive(Account actor, String adminId, boolean active) {
        requireRole(actor, Role.CHIEF_EVENT_COORDINATOR);
        Account admin = requireAccount(adminId);
        if (admin.role() != Role.ADMIN) throw new IllegalArgumentException("Selected account is not an administrator.");
        admin.setActive(active);
        database.saveAccounts(accounts);
    }

    public synchronized void resetAdministratorPassword(Account actor, String adminId, String password) {
        requireRole(actor, Role.CHIEF_EVENT_COORDINATOR);
        Account admin = requireAccount(adminId);
        if (admin.role() != Role.ADMIN) throw new IllegalArgumentException("Selected account is not an administrator.");
        admin.setPassword(password);
        database.saveAccounts(accounts);
        recordNotification(admin.id(), "Your administrator password was reset.");
    }

    public synchronized List<Account> administrators() {
        return accounts.stream().filter(a -> a.role() == Role.ADMIN).toList();
    }

    public synchronized List<Room> searchRooms(LocalDateTime start, LocalDateTime end,
                                               int capacity, String location) {
        validateFutureTimes(start, end);
        String requestedLocation = location == null ? "" : location.trim().toLowerCase(Locale.ROOT);
        return rooms.stream().filter(r -> r.status() == Room.Status.AVAILABLE)
                .filter(r -> r.capacity() >= capacity)
                .filter(r -> requestedLocation.isEmpty() || r.location().toLowerCase(Locale.ROOT).contains(requestedLocation))
                .filter(r -> isAvailable(r.id(), start, end, null))
                .sorted(Comparator.comparingInt(Room::capacity).thenComparing(Room::id)).toList();
    }

    public synchronized Room addRoom(Account actor, String id, String name, String location, int capacity) {
        requireAdmin(actor);
        if (roomById(id) != null) throw new IllegalArgumentException("Room ID already exists.");
        Room room = new Room(id, name, location, capacity, Room.Status.AVAILABLE);
        rooms.add(room);
        database.saveRooms(rooms);
        return room;
    }

    public synchronized void updateRoom(Account actor, String id, String name, String location, int capacity) {
        requireAdmin(actor);
        requireRoom(id).updateDetails(name, location, capacity);
        database.saveRooms(rooms);
    }

    public synchronized void setRoomStatus(Account actor, String id, Room.Status status) {
        requireAdmin(actor);
        requireRoom(id).setStatus(status);
        database.saveRooms(rooms);
    }

    public synchronized List<Room> rooms() { return List.copyOf(rooms); }

    public synchronized Booking bookRoom(Account user, String roomId, LocalDateTime start,
                                         LocalDateTime end, int attendees, String paymentMethod,
                                         String paymentDetails) {
        requireRole(user, Role.REGISTERED_USER);
        validateFutureTimes(start, end);
        Room room = requireRoom(roomId);
        if (room.status() != Room.Status.AVAILABLE) throw new IllegalStateException("Room is not bookable.");
        if (attendees > room.capacity()) throw new IllegalArgumentException("Attendees exceed room capacity.");
        if (!isAvailable(roomId, start, end, null)) throw new IllegalStateException("Room is no longer available.");
        if (paymentMethod.equalsIgnoreCase("Institutional Billing")
                && !user.organizationId().equalsIgnoreCase(paymentDetails == null ? "" : paymentDetails.trim()))
            throw new IllegalArgumentException("Institutional billing ID must match the account's organization ID.");
        BigDecimal oneHourDeposit = user.hourlyRate();
        PaymentReceipt receipt = PaymentStrategy.forMethod(paymentMethod).pay(oneHourDeposit, paymentDetails);
        Booking booking = new Booking(UUID.randomUUID().toString(), user.id(), roomId, start, end,
                attendees, BookingState.fromName("PENDING"), Booking.DepositStatus.PAID,
                receipt.method(), receipt.transactionId(), user.hourlyRate(), oneHourDeposit);
        attachObservers(booking);
        bookings.add(booking);
        booking.confirm();
        database.saveBookings(bookings);
        return booking;
    }

    public synchronized void editBooking(Account actor, String bookingId, String roomId,
                                         LocalDateTime start, LocalDateTime end, int attendees) {
        Booking booking = authorizedBooking(actor, bookingId);
        if (!LocalDateTime.now().isBefore(booking.start()))
            throw new IllegalStateException("After the start time, use the extension action instead of editing.");
        validateFutureTimes(start, end);
        Room room = requireRoom(roomId);
        if (room.status() != Room.Status.AVAILABLE || attendees > room.capacity())
            throw new IllegalArgumentException("The selected room cannot hold this booking.");
        if (!isAvailable(roomId, start, end, bookingId)) throw new IllegalStateException("Room is unavailable at that time.");
        booking.reschedule(roomId, start, end, attendees);
        database.saveBookings(bookings);
    }

    public synchronized void extendBooking(Account actor, String bookingId, LocalDateTime newEnd) {
        Booking booking = authorizedBooking(actor, bookingId);
        if (!LocalDateTime.now().isBefore(booking.end()))
            throw new IllegalStateException("This booking has already expired.");
        if (newEnd == null || !newEnd.isAfter(booking.end()))
            throw new IllegalArgumentException("The new expiry must be later than the current end time.");
        if (Duration.between(booking.start(), newEnd).compareTo(Duration.ofHours(12)) > 0)
            throw new IllegalArgumentException("A booking cannot exceed 12 hours.");
        if (!isAvailable(booking.roomId(), booking.end(), newEnd, booking.id()))
            throw new IllegalStateException("The room is unavailable during the requested extension.");
        booking.extendTo(newEnd);
        database.saveBookings(bookings);
    }

    public synchronized void cancelBooking(Account actor, String bookingId) {
        Booking booking = authorizedBooking(actor, bookingId);
        if (!LocalDateTime.now().isBefore(booking.start()))
            throw new IllegalStateException("A booking can only be cancelled before its start time.");
        booking.cancel();
        database.saveBookings(bookings);
    }

    public synchronized void checkIn(Account actor, String bookingId, LocalDateTime scannedAt) {
        Booking booking = authorizedBooking(actor, bookingId);
        if (scannedAt.isBefore(booking.start().minusMinutes(30)))
            throw new IllegalStateException("Check-in opens 30 minutes before the booking.");
        if (scannedAt.isAfter(booking.start().plusMinutes(30))) {
            booking.markNoShow();
            database.saveBookings(bookings);
            throw new IllegalStateException("Check-in was over 30 minutes late; the deposit was forfeited.");
        }
        booking.checkIn();
        requireRoom(booking.roomId()).sensor().reportOccupancy(true);
        database.saveBookings(bookings);
    }

    public synchronized void sensorCheckIn(Account operator, String roomId, String badgeAccountId,
                                           LocalDateTime scannedAt) {
        requireAdmin(operator);
        Room room = requireRoom(roomId);
        String accountId = room.sensor().scanBadge(badgeAccountId);
        Booking booking = bookings.stream().filter(b -> b.accountId().equals(accountId))
                .filter(b -> b.roomId().equals(roomId)).filter(b -> b.state().name().equals("CONFIRMED"))
                .filter(b -> !scannedAt.isBefore(b.start().minusMinutes(30)) && !scannedAt.isAfter(b.start().plusMinutes(30)))
                .findFirst().orElseThrow(() -> new IllegalStateException("No valid booking found for this badge and room."));
        checkIn(requireAccount(accountId), booking.id(), scannedAt);
    }

    public synchronized void reportOccupancy(Account operator, String roomId, boolean occupied) {
        requireAdmin(operator);
        requireRoom(roomId).sensor().reportOccupancy(occupied);
    }

    public synchronized List<Booking> bookingsFor(Account account) {
        requireLoggedIn(account);
        processDueBookings(LocalDateTime.now());
        return bookings.stream().filter(b -> account.role() != Role.REGISTERED_USER || b.accountId().equals(account.id()))
                .sorted(Comparator.comparing(Booking::start)).toList();
    }

    public synchronized List<Booking> roomSchedule(Account actor, String roomId) {
        requireAdmin(actor);
        return bookings.stream().filter(b -> b.roomId().equals(roomId))
                .sorted(Comparator.comparing(Booking::start)).toList();
    }

    public synchronized List<Notification> notificationsFor(Account account) {
        requireLoggedIn(account);
        return notifications.stream().filter(n -> n.accountId().equals(account.id()))
                .sorted(Comparator.comparing(Notification::createdAt).reversed()).toList();
    }

    synchronized void recordNotification(String accountId, String message) {
        notifications.add(new Notification(UUID.randomUUID().toString(), accountId, LocalDateTime.now(), message));
        database.saveNotifications(notifications);
    }

    synchronized void notifyAdministrators(String message) {
        accounts.stream().filter(a -> a.active() && a.role() != Role.REGISTERED_USER)
                .forEach(a -> notifications.add(new Notification(UUID.randomUUID().toString(), a.id(), LocalDateTime.now(), message)));
        database.saveNotifications(notifications);
    }

    private void attachObservers(Booking booking) {
        booking.attach(new AccountNotificationObserver(this, booking.accountId()));
        booking.attach(new AdministratorNotificationObserver(this));
    }

    private boolean isAvailable(String roomId, LocalDateTime start, LocalDateTime end, String ignoredBookingId) {
        return bookings.stream().filter(b -> b.roomId().equals(roomId))
                .filter(b -> ignoredBookingId == null || !b.id().equals(ignoredBookingId))
                .noneMatch(b -> b.overlaps(start, end));
    }

    private Booking authorizedBooking(Account actor, String bookingId) {
        requireLoggedIn(actor);
        Booking booking = bookings.stream().filter(b -> b.id().equals(bookingId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
        if (actor.role() == Role.REGISTERED_USER && !booking.accountId().equals(actor.id()))
            throw new SecurityException("You may only manage your own bookings.");
        return booking;
    }

    private static void validateFutureTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start))
            throw new IllegalArgumentException("End time must be after start time.");
        if (Duration.between(start, end).compareTo(Duration.ofHours(12)) > 0)
            throw new IllegalArgumentException("A booking cannot exceed 12 hours.");
        if (start.isBefore(LocalDateTime.now().minusMinutes(1)))
            throw new IllegalArgumentException("A booking must start in the future.");
    }

    private void processDueBookings(LocalDateTime now) {
        boolean changed = false;
        for (Booking booking : bookings) {
            if (booking.state().name().equals("CONFIRMED") && now.isAfter(booking.start().plusMinutes(30))) {
                booking.markNoShow();
                changed = true;
            } else if (booking.state().name().equals("CHECKED_IN") && !now.isBefore(booking.end())) {
                booking.complete();
                changed = true;
            }
        }
        if (changed) database.saveBookings(bookings);
    }

    private static void validateEmail(String email) {
        if (email == null || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Enter a valid email address.");
    }

    private static void validateOrganizationId(UserType userType, String organizationId) {
        String value = organizationId == null ? "" : organizationId.trim();
        if (userType == UserType.STUDENT) {
            if (!value.matches("\\d{9}"))
                throw new IllegalArgumentException("Student number must contain exactly 9 digits.");
        } else if (!value.matches("[A-Za-z0-9-]{6,20}")) {
            throw new IllegalArgumentException("Organization ID must contain 6–20 letters, numbers, or hyphens.");
        }
    }

    private Account findAccountByEmail(String email) {
        if (email == null) return null;
        return accounts.stream().filter(a -> a.email().equalsIgnoreCase(email.trim())).findFirst().orElse(null);
    }
    private Account requireAccount(String id) { return accounts.stream().filter(a -> a.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Account not found.")); }
    private Room roomById(String id) { return rooms.stream().filter(r -> r.id().equalsIgnoreCase(id == null ? "" : id.trim())).findFirst().orElse(null); }
    private Room requireRoom(String id) { Room room = roomById(id); if (room == null) throw new IllegalArgumentException("Room not found."); return room; }
    private static void requireLoggedIn(Account account) { if (account == null || !account.active()) throw new SecurityException("Authentication required."); }
    private static void requireRole(Account account, Role role) { requireLoggedIn(account); if (account.role() != role) throw new SecurityException(role.displayName() + " permission required."); }
    private static void requireAdmin(Account account) { requireLoggedIn(account); if (account.role() == Role.REGISTERED_USER) throw new SecurityException("Administrator permission required."); }
}

record GeneratedAdminCredentials(Account account, String temporaryPassword) {}
