package ca.yorku.eecs3311.roomscheduler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Singleton pattern: the entire application shares one synchronized CSV persistence gateway. */
public final class DatabaseManager {
    private static volatile DatabaseManager instance;
    private final Path dataDirectory;

    private DatabaseManager() {
        dataDirectory = Path.of(System.getProperty("roomscheduler.dataDir", "data"));
        try { Files.createDirectories(dataDirectory); }
        catch (IOException e) { throw new IllegalStateException("Cannot create data directory", e); }
    }

    public static DatabaseManager getInstance() {
        DatabaseManager result = instance;
        if (result == null) {
            synchronized (DatabaseManager.class) {
                result = instance;
                if (result == null) instance = result = new DatabaseManager();
            }
        }
        return result;
    }

    static synchronized void resetForTests() { instance = null; }

    public synchronized List<Account> loadAccounts() {
        List<Account> result = new ArrayList<>();
        for (List<String> row : read("accounts.csv")) {
            if (row.size() < 7 || row.get(0).equals("id")) continue;
            result.add(AccountFactory.restore(row.get(0), row.get(1), row.get(2), row.get(3),
                    Role.valueOf(row.get(4)), Boolean.parseBoolean(row.get(5)), Boolean.parseBoolean(row.get(6))));
        }
        return result;
    }

    public synchronized void saveAccounts(List<Account> accounts) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("id", "name", "email", "passwordHash", "role", "universityVerified", "active"));
        for (Account a : accounts) rows.add(List.of(a.id(), a.name(), a.email(), a.passwordHash(),
                a.role().name(), String.valueOf(a.universityVerified()), String.valueOf(a.active())));
        write("accounts.csv", rows);
    }

    public synchronized List<Room> loadRooms() {
        List<Room> result = new ArrayList<>();
        for (List<String> row : read("rooms.csv")) {
            if (row.size() < 5 || row.get(0).equals("id")) continue;
            result.add(new Room(row.get(0), row.get(1), row.get(2), Integer.parseInt(row.get(3)),
                    Room.Status.valueOf(row.get(4))));
        }
        return result;
    }

    public synchronized void saveRooms(List<Room> rooms) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("id", "name", "location", "capacity", "status"));
        for (Room r : rooms) rows.add(List.of(r.id(), r.name(), r.location(), String.valueOf(r.capacity()), r.status().name()));
        write("rooms.csv", rows);
    }

    public synchronized List<Booking> loadBookings() {
        List<Booking> result = new ArrayList<>();
        for (List<String> row : read("bookings.csv")) {
            if (row.size() < 11 || row.get(0).equals("id")) continue;
            result.add(new Booking(row.get(0), row.get(1), row.get(2), LocalDateTime.parse(row.get(3)),
                    LocalDateTime.parse(row.get(4)), Integer.parseInt(row.get(5)), BookingState.fromName(row.get(6)),
                    Booking.DepositStatus.valueOf(row.get(7)), row.get(8), row.get(9)));
        }
        return result;
    }

    public synchronized void saveBookings(List<Booking> bookings) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("id", "accountId", "roomId", "start", "end", "attendees", "state",
                "depositStatus", "paymentMethod", "transactionId", "version"));
        for (Booking b : bookings) rows.add(List.of(b.id(), b.accountId(), b.roomId(), b.start().toString(),
                b.end().toString(), String.valueOf(b.attendees()), b.state().name(), b.depositStatus().name(),
                b.paymentMethod(), b.transactionId(), "1"));
        write("bookings.csv", rows);
    }

    public synchronized List<Notification> loadNotifications() {
        List<Notification> result = new ArrayList<>();
        for (List<String> row : read("notifications.csv")) {
            if (row.size() < 4 || row.get(0).equals("id")) continue;
            result.add(new Notification(row.get(0), row.get(1), LocalDateTime.parse(row.get(2)), row.get(3)));
        }
        return result;
    }

    public synchronized void saveNotifications(List<Notification> notifications) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("id", "accountId", "createdAt", "message"));
        for (Notification n : notifications)
            rows.add(List.of(n.id(), n.accountId(), n.createdAt().toString(), n.message()));
        write("notifications.csv", rows);
    }

    private List<List<String>> read(String fileName) {
        Path file = dataDirectory.resolve(fileName);
        if (!Files.exists(file)) return List.of();
        try {
            List<List<String>> rows = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) rows.add(parseCsv(line));
            return rows;
        } catch (IOException e) { throw new IllegalStateException("Cannot read " + file, e); }
    }

    private void write(String fileName, List<List<String>> rows) {
        Path target = dataDirectory.resolve(fileName);
        Path temporary = dataDirectory.resolve(fileName + ".tmp");
        List<String> lines = rows.stream().map(DatabaseManager::formatCsv).toList();
        try {
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicNotSupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot write " + target, e); }
    }

    private static String formatCsv(List<String> fields) {
        return fields.stream().map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .reduce((a, b) -> a + "," + b).orElse("");
    }

    private static List<String> parseCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { fields.add(value.toString()); value.setLength(0); }
            else value.append(c);
        }
        fields.add(value.toString());
        return fields;
    }
}

