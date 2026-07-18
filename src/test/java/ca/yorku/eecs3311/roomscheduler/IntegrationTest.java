package ca.yorku.eecs3311.roomscheduler;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Dependency-free Req1–Req10 integration suite; exits non-zero on the first failure. */
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

        Account demoStudent = facade.authenticate("student@yorku.ca", "Student123!");
        Account admin = facade.authenticate("admin@yorku.ca", "Admin123!");
        Account chief = facade.authenticate("chief@yorku.ca", "Chief123!");
        check(demoStudent.role() == Role.REGISTERED_USER, "Factory creates registered users");
        check(admin.role() == Role.ADMIN, "Factory creates administrators");
        check(chief.role() == Role.CHIEF_EVENT_COORDINATOR, "Factory creates chief coordinator");

        // Req1 and Req8: user types, university verification, unique email, strong passwords and valid IDs.
        Account student = facade.register("Test, Student", "tester@yorku.ca", "Student1!",
                UserType.STUDENT, "218882191");
        Account faculty = facade.register("Faculty User", "faculty@yorku.ca", "Faculty1!",
                UserType.FACULTY, "FAC-1001");
        Account staff = facade.register("Staff User", "staff@yorku.ca", "Staff123!",
                UserType.STAFF, "STAFF-1001");
        Account partner = facade.register("Partner User", "partner@example.com", "Partner1!",
                UserType.PARTNER, "PARTNER-42");
        check(student.universityVerified() && faculty.universityVerified() && staff.universityVerified(),
                "University accounts are verified");
        check(!partner.universityVerified(), "External partner remains non-university account");
        check(student.userType() == UserType.STUDENT && partner.userType() == UserType.PARTNER,
                "All registered account categories are retained");
        check(student.organizationId().equals("218882191"), "Student number is retained");
        check(partner.organizationId().equals("PARTNER-42"), "Organization ID is retained");
        expectFailure(() -> facade.register("Weak", "weak@yorku.ca", "weakpass",
                UserType.STUDENT, "218882192"), "Weak password rejected");
        expectFailure(() -> facade.register("Bad Student", "bad@example.com", "Student1!",
                UserType.STUDENT, "218882193"), "University type requires verified York email");
        expectFailure(() -> facade.register("Bad ID", "badid@yorku.ca", "Student1!",
                UserType.STUDENT, "123"), "Invalid student number rejected");
        expectFailure(() -> facade.register("Duplicate", "tester@yorku.ca", "Student1!",
                UserType.STUDENT, "218882194"), "Duplicate email rejected");

        // Req3, Req4 and Req10: availability, category rates, one-hour deposit and payment choices.
        check(student.hourlyRate().compareTo(new BigDecimal("20.00")) == 0, "Student rate is $20/hour");
        check(faculty.hourlyRate().compareTo(new BigDecimal("30.00")) == 0, "Faculty rate is $30/hour");
        check(staff.hourlyRate().compareTo(new BigDecimal("40.00")) == 0, "Staff rate is $40/hour");
        check(partner.hourlyRate().compareTo(new BigDecimal("50.00")) == 0, "Partner rate is $50/hour");

        LocalDateTime start = LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = start.plusHours(2);
        List<Room> available = facade.searchRooms(start, end, 10, "Lassonde");
        check(!available.isEmpty(), "Rooms filter by time, capacity, location and status");
        expectFailure(() -> facade.searchRooms(start, start.plusHours(12).plusMinutes(1), 1, ""),
                "Booking duration boundary is enforced precisely");
        Room room = available.get(0);
        Booking booking = facade.bookRoom(student, room.id(), start, end, 10,
                "Credit Card", "4111111111111111");
        check(booking.state().name().equals("CONFIRMED"), "Booking confirms after payment");
        check(booking.depositAmount().compareTo(new BigDecimal("20.00")) == 0,
                "Upfront student deposit equals one hour's fee");
        check(booking.estimatedTotal().compareTo(new BigDecimal("40.00")) == 0,
                "Two-hour student booking costs $40");
        check(booking.paymentMethod().equals("Credit Card"), "Credit card strategy used");
        check(facade.notificationsFor(student).stream().anyMatch(n -> n.message().contains("confirmed")),
                "Observer notifies booking owner");
        check(facade.notificationsFor(admin).stream().anyMatch(n -> n.message().contains(booking.id())),
                "Observer notifies administrators");
        expectFailure(() -> facade.bookRoom(partner, room.id(), start.plusMinutes(30), end, 2,
                "Debit", "5555555555555555"), "Overlapping booking rejected");
        expectFailure(() -> facade.bookRoom(admin, room.id(), start.plusDays(1), end.plusDays(1), 1,
                "Credit Card", "4111111111111111"), "Only registered users book rooms");

        Booking facultyBooking = facade.bookRoom(faculty, "ACE-201", start.plusDays(1), start.plusDays(1).plusHours(1),
                2, "Debit", "5555555555555555");
        check(facultyBooking.depositAmount().compareTo(new BigDecimal("30.00")) == 0,
                "Faculty one-hour deposit is $30");
        Booking staffBooking = facade.bookRoom(staff, "DB-001", start.plusDays(1), start.plusDays(1).plusHours(1),
                2, "Credit Card", "4111111111111111");
        check(staffBooking.depositAmount().compareTo(new BigDecimal("40.00")) == 0,
                "Staff one-hour deposit is $40");
        Booking partnerBooking = facade.bookRoom(partner, "ACE-201", start.plusDays(2), start.plusDays(2).plusHours(1),
                2, "Institutional Billing", "PARTNER-42");
        check(partnerBooking.depositAmount().compareTo(new BigDecimal("50.00")) == 0,
                "Partner one-hour deposit is $50");
        check(partnerBooking.paymentMethod().equals("Institutional Billing"), "Institutional billing strategy used");
        expectFailure(() -> facade.bookRoom(partner, "DB-001", start.plusDays(2), start.plusDays(2).plusHours(1),
                1, "Institutional Billing", "WRONG-ID"), "Institutional billing ID must match account");

        // Req8 and Req9: edit/cancel before start and extend before expiry only when available.
        facade.editBooking(student, booking.id(), room.id(), start.plusMinutes(30), end.plusMinutes(30), 8);
        check(booking.start().equals(start.plusMinutes(30)), "Booking can be edited before start");
        facade.extendBooking(student, booking.id(), booking.end().plusMinutes(30));
        check(booking.end().equals(end.plusHours(1)), "Booking can be extended before expiry");
        Booking blocking = facade.bookRoom(partner, room.id(), booking.end(), booking.end().plusHours(1), 2,
                "Debit", "5555555555555555");
        expectFailure(() -> facade.extendBooking(student, booking.id(), booking.end().plusMinutes(30)),
                "Extension rejected when room is unavailable");
        facade.cancelBooking(student, booking.id());
        check(booking.state().name().equals("CANCELLED"), "Booking cancels before start");
        check(booking.depositStatus() == Booking.DepositStatus.REFUNDED, "Cancellation refunds deposit");
        expectFailure(() -> facade.checkIn(student, booking.id(), start), "Cancelled booking cannot check in");

        // Req6 and Req7: unique room ID, details and complete administration lifecycle.
        expectFailure(() -> facade.addRoom(student, "TEST-1", "Forbidden", "Nowhere", 4),
                "Registered user cannot manage rooms");
        Room testRoom = facade.addRoom(admin, "TEST-1", "Test Room", "Test Building 101", 6);
        expectFailure(() -> facade.addRoom(admin, "TEST-1", "Duplicate", "Elsewhere", 3),
                "Duplicate room ID rejected");
        facade.updateRoom(admin, testRoom.id(), "Updated Test Room", "Test Building 202", 7);
        facade.setRoomStatus(admin, testRoom.id(), Room.Status.MAINTENANCE);
        check(testRoom.status() == Room.Status.MAINTENANCE && testRoom.capacity() == 7,
                "Admin updates details and closes room for maintenance");
        facade.setRoomStatus(admin, testRoom.id(), Room.Status.DISABLED);
        check(testRoom.status() == Room.Status.DISABLED, "Admin disables room");
        facade.setRoomStatus(admin, testRoom.id(), Room.Status.AVAILABLE);
        check(testRoom.status() == Room.Status.AVAILABLE, "Admin enables room");

        // Req2: only the chief auto-generates complete administrator credentials.
        GeneratedAdminCredentials generated = facade.generateAdministrator(chief, "Generated Admin");
        check(generated.account().role() == Role.ADMIN, "Chief auto-generates administrator account");
        check(generated.account().email().startsWith("admin.") && generated.account().email().endsWith("@yorku.ca"),
                "Administrator email is auto-generated");
        check(facade.authenticate(generated.account().email(), generated.temporaryPassword()) == generated.account(),
                "Auto-generated credentials authenticate");
        expectFailure(() -> facade.generateAdministrator(admin, "Forbidden"),
                "Only chief can auto-generate administrators");
        facade.setAdministratorActive(chief, generated.account().id(), false);
        expectFailure(() -> facade.authenticate(generated.account().email(), generated.temporaryPassword()),
                "Deactivated administrator cannot log in");
        facade.setAdministratorActive(chief, generated.account().id(), true);
        facade.resetAdministratorPassword(chief, generated.account().id(), "NewAdmin1!");
        check(facade.authenticate(generated.account().email(), "NewAdmin1!") == generated.account(),
                "Chief resets administrator password");

        // Req5 and Req4: sensor badge verification, occupancy, timely check-in and forfeiture.
        LocalDateTime imminent = LocalDateTime.now().plusMinutes(10).truncatedTo(ChronoUnit.MINUTES);
        Booking sensorBooking = facade.bookRoom(student, testRoom.id(), imminent, imminent.plusHours(1), 3,
                "Debit", "5555555555555555");
        facade.sensorCheckIn(admin, testRoom.id(), student.id(), LocalDateTime.now());
        check(sensorBooking.state().name().equals("CHECKED_IN"), "Room sensor verifies badge and checks in booking");
        check(testRoom.sensor().occupied(), "Sensor sends occupancy data to system");
        check(sensorBooking.depositStatus() == Booking.DepositStatus.APPLIED_TO_COST,
                "Timely check-in applies deposit to final cost");

        LocalDateTime lateStart = LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.MINUTES);
        Room lateRoom = facade.rooms().stream().filter(r -> !r.id().equals(testRoom.id())).findFirst().orElseThrow();
        Booking late = facade.bookRoom(partner, lateRoom.id(), lateStart, lateStart.plusHours(1), 1,
                "Debit", "5555555555555555");
        expectFailure(() -> facade.checkIn(partner, late.id(), lateStart.plusMinutes(31)),
                "Check-in over 30 minutes late is rejected");
        check(late.depositStatus() == Booking.DepositStatus.FORFEITED,
                "Late/no-show booking forfeits one-hour deposit");

        // CSV persistence, backward-safe quoting, and financial fields.
        int bookingsBeforeReload = facade.bookingsFor(admin).size();
        SchedulerFacade.resetForTests();
        SchedulerFacade reloaded = SchedulerFacade.getInstance();
        Account reloadedStudent = reloaded.authenticate("tester@yorku.ca", "Student1!");
        check(reloadedStudent.name().equals("Test, Student"), "Quoted CSV values reload correctly");
        check(reloadedStudent.userType() == UserType.STUDENT
                        && reloadedStudent.organizationId().equals("218882191"),
                "Account category and ID persist");
        List<Booking> reloadedBookings = reloaded.bookingsFor(reloaded.authenticate("admin@yorku.ca", "Admin123!"));
        check(reloadedBookings.size() == bookingsBeforeReload, "Bookings persist and reload from CSV");
        check(reloadedBookings.stream().anyMatch(b -> b.id().equals(partnerBooking.id())
                        && b.hourlyRate().compareTo(new BigDecimal("50.00")) == 0),
                "Rates and deposits persist");
        check(reloaded.rooms().stream().anyMatch(r -> r.id().equals("TEST-1")),
                "Rooms persist and reload from CSV");

        Path legacyData = Path.of("out", "legacy-test-data");
        Files.createDirectories(legacyData);
        Files.writeString(legacyData.resolve("accounts.csv"),
                "\"id\",\"name\",\"email\",\"passwordHash\",\"role\",\"universityVerified\",\"active\"\n"
                        + "\"legacy-student\",\"Demo Student\",\"student@yorku.ca\",\""
                        + legacyDigest("student123")
                        + "\",\"REGISTERED_USER\",\"true\",\"true\"\n",
                StandardCharsets.UTF_8);
        System.setProperty("roomscheduler.dataDir", legacyData.toString());
        SchedulerFacade.resetForTests();
        SchedulerFacade migrated = SchedulerFacade.getInstance();
        check(migrated.authenticate("student@yorku.ca", "Student123!").email().equals("student@yorku.ca"),
                "Legacy demo password migrates to the new strong credential");

        System.out.println("PASS: " + assertions + " Req1–Req10 integration assertions");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError("FAILED: " + message);
        System.out.println("  OK  " + message);
    }

    private static void expectFailure(Runnable action, String message) {
        assertions++;
        try { action.run(); }
        catch (RuntimeException expected) {
            System.out.println("  OK  " + message + " (" + expected.getMessage() + ")");
            return;
        }
        throw new AssertionError("FAILED: " + message);
    }

    private static String legacyDigest(String password) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
