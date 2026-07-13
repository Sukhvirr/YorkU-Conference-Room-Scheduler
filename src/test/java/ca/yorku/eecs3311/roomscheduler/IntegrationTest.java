package ca.yorku.eecs3311.roomscheduler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/** Dependency-free integration test runner; exits non-zero on the first failed requirement. */
public final class IntegrationTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        Path data = Path.of("out", "test-data");
        if (Files.exists(data)) {
            try (var files = Files.walk(data)) {
                files.sorted(Comparator.reverseOrder()).filter(path -> !path.equals(data)).forEach(path -> {
                    try { Files.delete(path); } catch (Exception e) { throw new RuntimeException(e); }
                });
            }
        }
        Files.createDirectories(data);
        System.setProperty("roomscheduler.dataDir", data.toString());
        SchedulerFacade.resetForTests();

        SchedulerFacade facade = SchedulerFacade.getInstance();
        check(DatabaseManager.getInstance() == DatabaseManager.getInstance(), "Singleton database instance");

        Account student = facade.authenticate("student@yorku.ca", "student123");
        Account admin = facade.authenticate("admin@yorku.ca", "admin123");
        Account chief = facade.authenticate("chief@yorku.ca", "chief123");
        check(student.role() == Role.REGISTERED_USER, "Registered user factory");
        check(admin.role() == Role.ADMIN, "Admin factory");
        check(chief.role() == Role.CHIEF_EVENT_COORDINATOR, "Chief factory");

        Account yorkUser = facade.register("Test, Student", "tester@yorku.ca", "secret1", true);
        Account guest = facade.register("Community Guest", "guest@example.com", "secret2", false);
        check(yorkUser.universityVerified(), "York account verification");
        check(!guest.universityVerified(), "Optional verification for general account");
        expectFailure(() -> facade.register("Duplicate", "tester@yorku.ca", "secret1", true), "Duplicate email rejected");
        expectFailure(() -> facade.register("Fake", "fake@example.com", "secret1", true), "Non-York verification rejected");

        LocalDateTime start = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = start.plusHours(2);
        List<Room> available = facade.searchRooms(start, end, 10, "Lassonde");
        check(!available.isEmpty(), "Availability search by time/capacity/location");
        Room room = available.get(0);
        Booking booking = facade.bookRoom(yorkUser, room.id(), start, end, 10,
                "Credit Card", "4111111111111111");
        check(booking.state().name().equals("CONFIRMED"), "Pending-to-confirmed State transition");
        check(booking.depositStatus() == Booking.DepositStatus.PAID, "$10 deposit paid");
        check(facade.notificationsFor(yorkUser).stream().anyMatch(n -> n.message().contains("confirmed")),
                "Observer notified booking owner");
        check(facade.notificationsFor(admin).stream().anyMatch(n -> n.message().contains(booking.id())),
                "Observer notified administrators");
        expectFailure(() -> facade.bookRoom(guest, room.id(), start.plusMinutes(30), end, 2,
                "Debit", "5555555555555555"), "Overlapping booking rejected");

        facade.editBooking(yorkUser, booking.id(), room.id(), start.plusHours(1), end.plusHours(1), 8);
        check(booking.start().equals(start.plusHours(1)), "Booking edit/extension");
        facade.cancelBooking(yorkUser, booking.id());
        check(booking.state().name().equals("CANCELLED"), "Confirmed-to-cancelled State transition");
        check(booking.depositStatus() == Booking.DepositStatus.REFUNDED, "Cancellation refunds deposit");
        expectFailure(() -> facade.checkIn(yorkUser, booking.id(), start.plusHours(1)), "Cancelled booking cannot check in");

        expectFailure(() -> facade.addRoom(student, "TEST-1", "Forbidden", "Nowhere", 4), "User cannot manage rooms");
        Room testRoom = facade.addRoom(admin, "TEST-1", "Test Room", "Test Building", 6);
        facade.updateRoom(admin, testRoom.id(), "Updated Test Room", "Test Building", 7);
        facade.setRoomStatus(admin, testRoom.id(), Room.Status.MAINTENANCE);
        check(testRoom.status() == Room.Status.MAINTENANCE && testRoom.capacity() == 7, "Administrator room management");
        facade.setRoomStatus(admin, testRoom.id(), Room.Status.AVAILABLE);

        Account generatedAdmin = facade.createAdministrator(chief, "Generated Admin", "generated@yorku.ca", "admin456");
        check(generatedAdmin.role() == Role.ADMIN, "Chief generates administrator credentials");
        expectFailure(() -> facade.createAdministrator(admin, "Forbidden", "forbidden@yorku.ca", "admin456"),
                "Only chief generates administrators");
        facade.setAdministratorActive(chief, generatedAdmin.id(), false);
        expectFailure(() -> facade.authenticate("generated@yorku.ca", "admin456"), "Deactivated admin cannot log in");
        facade.setAdministratorActive(chief, generatedAdmin.id(), true);
        facade.resetAdministratorPassword(chief, generatedAdmin.id(), "newpass1");
        check(facade.authenticate("generated@yorku.ca", "newpass1") == generatedAdmin, "Chief resets admin password");

        LocalDateTime imminent = LocalDateTime.now().plusMinutes(10).truncatedTo(ChronoUnit.MINUTES);
        Booking sensorBooking = facade.bookRoom(yorkUser, testRoom.id(), imminent, imminent.plusHours(1), 3,
                "Campus Card", "218882191");
        facade.sensorCheckIn(admin, testRoom.id(), yorkUser.id(), LocalDateTime.now());
        check(sensorBooking.state().name().equals("CHECKED_IN"), "Badge sensor checks in valid booking");
        check(testRoom.sensor().occupied(), "Room sensor reports occupancy");
        check(sensorBooking.depositStatus() == Booking.DepositStatus.APPLIED_TO_COST, "Checked-in deposit applied to cost");

        LocalDateTime lateStart = LocalDateTime.now().plusDays(4).truncatedTo(ChronoUnit.MINUTES);
        Room lateRoom = facade.rooms().stream().filter(r -> !r.id().equals(testRoom.id())).findFirst().orElseThrow();
        Booking late = facade.bookRoom(guest, lateRoom.id(), lateStart, lateStart.plusHours(1), 1,
                "Debit", "5555555555555555");
        expectFailure(() -> facade.checkIn(guest, late.id(), lateStart.plusMinutes(31)), "Late check-in rejected");
        check(late.depositStatus() == Booking.DepositStatus.FORFEITED, "Late check-in forfeits deposit");

        int bookingsBeforeReload = facade.bookingsFor(admin).size();
        SchedulerFacade.resetForTests();
        SchedulerFacade reloaded = SchedulerFacade.getInstance();
        Account reloadedTester = reloaded.authenticate("tester@yorku.ca", "secret1");
        check(reloadedTester.name().equals("Test, Student"), "Quoted CSV values reload correctly");
        check(reloaded.bookingsFor(reloaded.authenticate("admin@yorku.ca", "admin123")).size() == bookingsBeforeReload,
                "Bookings persist and reload from CSV");
        check(reloaded.rooms().stream().anyMatch(r -> r.id().equals("TEST-1")), "Rooms persist and reload from CSV");

        System.out.println("PASS: " + assertions + " integration assertions");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError("FAILED: " + message);
        System.out.println("  OK  " + message);
    }

    private static void expectFailure(Runnable action, String message) {
        assertions++;
        try { action.run(); }
        catch (RuntimeException expected) { System.out.println("  OK  " + message + " (" + expected.getMessage() + ")"); return; }
        throw new AssertionError("FAILED: " + message);
    }
}
