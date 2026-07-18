package ca.yorku.eecs3311.roomscheduler;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class MainFrame extends JFrame {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final SchedulerFacade scheduler = SchedulerFacade.getInstance();
    private final JPanel root = new JPanel(new CardLayout());
    private Account currentAccount;

    public MainFrame() {
        super("YorkU Conference Room Scheduler");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setContentPane(root);
        showAuthentication();
        pack();
        setLocationRelativeTo(null);
    }

    private void showAuthentication() {
        root.removeAll();
        JPanel page = new JPanel(new BorderLayout(20, 20));
        page.setBorder(BorderFactory.createEmptyBorder(35, 80, 35, 80));
        JLabel title = new JLabel("YorkU Conference Room Scheduler", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        page.add(title, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Log in", loginPanel());
        tabs.addTab("Create account", registrationPanel());
        page.add(tabs, BorderLayout.CENTER);
        JLabel demos = new JLabel("Demo logins: student@yorku.ca / Student123!   |   admin@yorku.ca / Admin123!   |   chief@yorku.ca / Chief123!",
                SwingConstants.CENTER);
        page.add(demos, BorderLayout.SOUTH);
        root.add(page, "auth");
        ((CardLayout) root.getLayout()).show(root, "auth");
        root.revalidate();
        root.repaint();
    }

    private JPanel loginPanel() {
        JPanel panel = formPanel();
        JTextField email = new JTextField("student@yorku.ca", 25);
        JPasswordField password = new JPasswordField("Student123!", 25);
        addFormRow(panel, 0, "Email", email);
        addFormRow(panel, 1, "Password", password);
        JButton login = new JButton("Log in");
        GridBagConstraints c = constraints(2); c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        panel.add(login, c);
        login.addActionListener(e -> runAction(() -> {
            currentAccount = scheduler.authenticate(email.getText(), new String(password.getPassword()));
            showDashboard();
        }));
        return panel;
    }

    private JPanel registrationPanel() {
        JPanel panel = formPanel();
        JTextField name = new JTextField(25);
        JTextField email = new JTextField(25);
        JPasswordField password = new JPasswordField(25);
        JComboBox<UserType> userType = new JComboBox<>(UserType.values());
        JTextField organizationId = new JTextField(25);
        addFormRow(panel, 0, "Full name", name);
        addFormRow(panel, 1, "Email", email);
        addFormRow(panel, 2, "Account type", userType);
        addFormRow(panel, 3, "Student / organization ID", organizationId);
        addFormRow(panel, 4, "Strong password", password);
        JLabel passwordHelp = new JLabel("8+ characters with uppercase, lowercase, number, and symbol");
        GridBagConstraints uc = constraints(5); uc.gridx = 1; uc.anchor = GridBagConstraints.WEST;
        panel.add(passwordHelp, uc);
        JButton register = new JButton("Create account");
        GridBagConstraints bc = constraints(6); bc.gridx = 1; bc.anchor = GridBagConstraints.WEST;
        panel.add(register, bc);
        register.addActionListener(e -> runAction(() -> {
            currentAccount = scheduler.register(name.getText(), email.getText(),
                    new String(password.getPassword()), (UserType) userType.getSelectedItem(),
                    organizationId.getText());
            showDashboard();
        }));
        return panel;
    }

    private void showDashboard() {
        root.removeAll();
        JPanel page = new JPanel(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel welcome = new JLabel("Welcome, " + currentAccount.name() + " — " + currentAccount.role().displayName());
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 17f));
        JButton logout = new JButton("Log out");
        logout.addActionListener(e -> { currentAccount = null; showAuthentication(); });
        header.add(welcome, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        if (currentAccount.role() == Role.REGISTERED_USER)
            tabs.addTab("Find & Book Rooms", roomSearchPanel());
        tabs.addTab(currentAccount.role() == Role.REGISTERED_USER ? "My Bookings" : "All Bookings", bookingsPanel());
        tabs.addTab("Notifications", notificationsPanel());
        if (currentAccount.role() != Role.REGISTERED_USER) {
            tabs.addTab("Room Administration", roomAdministrationPanel());
            tabs.addTab("Room Sensors", sensorPanel());
        }
        if (currentAccount.role() == Role.CHIEF_EVENT_COORDINATOR)
            tabs.addTab("Administrator Accounts", administratorPanel());
        page.add(tabs, BorderLayout.CENTER);
        root.add(page, "dashboard");
        ((CardLayout) root.getLayout()).show(root, "dashboard");
        root.revalidate();
        root.repaint();
    }

    private JPanel roomSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        LocalDateTime defaultStart = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES);
        JTextField start = new JTextField(DATE_TIME.format(defaultStart), 16);
        JTextField end = new JTextField(DATE_TIME.format(defaultStart.plusHours(1)), 16);
        JSpinner capacity = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
        JTextField location = new JTextField(12);
        JButton search = new JButton("Search available rooms");
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Start")); filters.add(start); filters.add(new JLabel("End")); filters.add(end);
        filters.add(new JLabel("Attendees")); filters.add(capacity); filters.add(new JLabel("Location contains"));
        filters.add(location); filters.add(search);
        panel.add(filters, BorderLayout.NORTH);

        DefaultTableModel model = tableModel("Room ID", "Name", "Location", "Capacity", "Status", "Your hourly rate");
        JTable table = new JTable(model);
        List<Room> results = new ArrayList<>();
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton book = new JButton("Book selected room (one-hour deposit)");
        book.setEnabled(false);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT)); footer.add(book); panel.add(footer, BorderLayout.SOUTH);

        Runnable refresh = () -> runAction(() -> {
            results.clear();
            results.addAll(scheduler.searchRooms(parseDate(start.getText()), parseDate(end.getText()),
                    (Integer) capacity.getValue(), location.getText()));
            model.setRowCount(0);
            for (Room r : results) model.addRow(new Object[]{r.id(), r.name(), r.location(), r.capacity(), r.status(),
                    "$" + currentAccount.hourlyRate()});
            book.setEnabled(!results.isEmpty());
        });
        search.addActionListener(e -> refresh.run());
        book.addActionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected < 0) { showError("Select a room first."); return; }
            Room room = results.get(table.convertRowIndexToModel(selected));
            showPaymentDialog(room, parseDateSafe(start.getText()), parseDateSafe(end.getText()), (Integer) capacity.getValue(), refresh);
        });
        refresh.run();
        return panel;
    }

    private void showPaymentDialog(Room room, LocalDateTime start, LocalDateTime end, int attendees, Runnable after) {
        if (start == null || end == null) return;
        JComboBox<String> method = new JComboBox<>(new String[]{"Credit Card", "Debit", "Institutional Billing"});
        JTextField details = new JTextField("4111111111111111", 18);
        method.addActionListener(e -> {
            if ("Institutional Billing".equals(method.getSelectedItem())) details.setText(currentAccount.organizationId());
            else details.setText("4111111111111111");
        });
        JPanel form = new JPanel(); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JLabel("Room: " + room));
        form.add(new JLabel("Time: " + DATE_TIME.format(start) + " to " + DATE_TIME.format(end)));
        long minutes = java.time.Duration.between(start, end).toMinutes();
        java.math.BigDecimal total = currentAccount.hourlyRate()
                .multiply(java.math.BigDecimal.valueOf(minutes))
                .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        form.add(new JLabel("Hourly rate: $" + currentAccount.hourlyRate()));
        form.add(new JLabel("One-hour upfront deposit: $" + currentAccount.hourlyRate()));
        form.add(new JLabel("Estimated total: $" + total));
        form.add(new JLabel("Payment method")); form.add(method);
        form.add(new JLabel("Card number or institutional billing ID")); form.add(details);
        int answer = JOptionPane.showConfirmDialog(this, form, "Pay one-hour deposit and book", JOptionPane.OK_CANCEL_OPTION);
        if (answer == JOptionPane.OK_OPTION) runAction(() -> {
            Booking booking = scheduler.bookRoom(currentAccount, room.id(), start, end, attendees,
                    (String) method.getSelectedItem(), details.getText());
            JOptionPane.showMessageDialog(this, "Booking confirmed. ID: " + booking.id());
            after.run();
        });
    }

    private JPanel bookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        DefaultTableModel model = tableModel("Booking ID", "User ID", "Room", "Start", "Expiry", "People",
                "State", "Rate", "Deposit", "Estimated total");
        JTable table = new JTable(model);
        List<Booking> rows = new ArrayList<>();
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton refreshButton = new JButton("Refresh");
        JButton edit = new JButton("Edit Booking");
        JButton extend = new JButton("Extend Expiry");
        JButton cancel = new JButton("Cancel");
        JButton checkIn = new JButton("Scan badge / Check in now");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refreshButton); actions.add(edit); actions.add(extend); actions.add(cancel); actions.add(checkIn);
        panel.add(actions, BorderLayout.SOUTH);
        Runnable refresh = () -> {
            rows.clear(); rows.addAll(scheduler.bookingsFor(currentAccount)); model.setRowCount(0);
            for (Booking b : rows) model.addRow(new Object[]{shortId(b.id()), shortId(b.accountId()), b.roomId(),
                    DATE_TIME.format(b.start()), DATE_TIME.format(b.end()), b.attendees(), b.state().name(),
                    "$" + b.hourlyRate(), "$" + b.depositAmount() + " " + b.depositStatus(), "$" + b.estimatedTotal()});
        };
        refreshButton.addActionListener(e -> runAction(refresh));
        edit.addActionListener(e -> withSelected(table, rows, booking -> editBookingDialog(booking, refresh)));
        extend.addActionListener(e -> withSelected(table, rows, booking -> extendBookingDialog(booking, refresh)));
        cancel.addActionListener(e -> withSelected(table, rows, booking -> runAction(() -> {
            scheduler.cancelBooking(currentAccount, booking.id()); refresh.run();
        })));
        checkIn.addActionListener(e -> withSelected(table, rows, booking -> runAction(() -> {
            scheduler.checkIn(currentAccount, booking.id(), LocalDateTime.now()); refresh.run();
        })));
        refresh.run();
        return panel;
    }

    private void editBookingDialog(Booking booking, Runnable refresh) {
        JComboBox<Room> room = new JComboBox<>(scheduler.rooms().toArray(Room[]::new));
        for (int i = 0; i < room.getItemCount(); i++) if (room.getItemAt(i).id().equals(booking.roomId())) room.setSelectedIndex(i);
        JTextField start = new JTextField(DATE_TIME.format(booking.start()), 17);
        JTextField end = new JTextField(DATE_TIME.format(booking.end()), 17);
        JSpinner people = new JSpinner(new SpinnerNumberModel(booking.attendees(), 1, 500, 1));
        JPanel form = formPanel();
        addFormRow(form, 0, "Room", room); addFormRow(form, 1, "Start", start);
        addFormRow(form, 2, "End", end); addFormRow(form, 3, "Attendees", people);
        if (JOptionPane.showConfirmDialog(this, form, "Edit Booking", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            runAction(() -> {
                scheduler.editBooking(currentAccount, booking.id(), ((Room) room.getSelectedItem()).id(),
                        parseDate(start.getText()), parseDate(end.getText()), (Integer) people.getValue());
                refresh.run();
            });
    }

    private void extendBookingDialog(Booking booking, Runnable refresh) {
        JTextField newEnd = new JTextField(DATE_TIME.format(booking.end().plusHours(1)), 17);
        JPanel form = formPanel();
        addFormRow(form, 0, "Current expiry", new JLabel(DATE_TIME.format(booking.end())));
        addFormRow(form, 1, "New expiry", newEnd);
        if (JOptionPane.showConfirmDialog(this, form, "Extend Booking Before Expiry", JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION)
            runAction(() -> {
                scheduler.extendBooking(currentAccount, booking.id(), parseDate(newEnd.getText()));
                refresh.run();
            });
    }

    private JPanel notificationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTextArea messages = new JTextArea(); messages.setEditable(false); messages.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JButton refresh = new JButton("Refresh notifications");
        Runnable update = () -> {
            StringBuilder text = new StringBuilder();
            for (Notification n : scheduler.notificationsFor(currentAccount))
                text.append(DATE_TIME.format(n.createdAt())).append("  ").append(n.message()).append('\n');
            messages.setText(text.toString());
        };
        refresh.addActionListener(e -> runAction(update));
        panel.add(refresh, BorderLayout.NORTH); panel.add(new JScrollPane(messages), BorderLayout.CENTER);
        update.run(); return panel;
    }

    private JPanel roomAdministrationPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        DefaultTableModel model = tableModel("Room ID", "Name", "Location", "Capacity", "Status", "Sensor Occupied");
        JTable table = new JTable(model); panel.add(new JScrollPane(table), BorderLayout.CENTER);
        List<Room> rows = new ArrayList<>();
        Runnable refresh = () -> { rows.clear(); rows.addAll(scheduler.rooms()); model.setRowCount(0);
            for (Room r : rows) model.addRow(new Object[]{r.id(), r.name(), r.location(), r.capacity(), r.status(), r.sensor().occupied()}); };
        JButton add = new JButton("Add room"); JButton update = new JButton("Update selected");
        JButton available = new JButton("Enable"); JButton disable = new JButton("Disable");
        JButton maintenance = new JButton("Close for maintenance"); JButton schedule = new JButton("View schedule");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (JButton b : List.of(add, update, available, disable, maintenance, schedule)) actions.add(b);
        panel.add(actions, BorderLayout.SOUTH);
        add.addActionListener(e -> roomDialog(null, refresh));
        update.addActionListener(e -> withSelected(table, rows, r -> roomDialog(r, refresh)));
        available.addActionListener(e -> changeRoomStatus(table, rows, Room.Status.AVAILABLE, refresh));
        disable.addActionListener(e -> changeRoomStatus(table, rows, Room.Status.DISABLED, refresh));
        maintenance.addActionListener(e -> changeRoomStatus(table, rows, Room.Status.MAINTENANCE, refresh));
        schedule.addActionListener(e -> withSelected(table, rows, r -> showRoomSchedule(r)));
        refresh.run(); return panel;
    }

    private void roomDialog(Room existing, Runnable refresh) {
        JTextField id = new JTextField(existing == null ? "" : existing.id(), 18); id.setEnabled(existing == null);
        JTextField name = new JTextField(existing == null ? "" : existing.name(), 18);
        JTextField location = new JTextField(existing == null ? "" : existing.location(), 18);
        JSpinner capacity = new JSpinner(new SpinnerNumberModel(existing == null ? 1 : existing.capacity(), 1, 500, 1));
        JPanel form = formPanel(); addFormRow(form, 0, "Room ID", id); addFormRow(form, 1, "Name", name);
        addFormRow(form, 2, "Location", location); addFormRow(form, 3, "Capacity", capacity);
        if (JOptionPane.showConfirmDialog(this, form, existing == null ? "Add Room" : "Update Room", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            runAction(() -> {
                if (existing == null) scheduler.addRoom(currentAccount, id.getText(), name.getText(), location.getText(), (Integer) capacity.getValue());
                else scheduler.updateRoom(currentAccount, existing.id(), name.getText(), location.getText(), (Integer) capacity.getValue());
                refresh.run();
            });
    }

    private void changeRoomStatus(JTable table, List<Room> rows, Room.Status status, Runnable refresh) {
        withSelected(table, rows, r -> runAction(() -> { scheduler.setRoomStatus(currentAccount, r.id(), status); refresh.run(); }));
    }

    private void showRoomSchedule(Room room) {
        StringBuilder schedule = new StringBuilder("Schedule for ").append(room).append("\n\n");
        for (Booking b : scheduler.roomSchedule(currentAccount, room.id()))
            schedule.append(DATE_TIME.format(b.start())).append(" – ").append(DATE_TIME.format(b.end()))
                    .append("  ").append(b.state().name()).append("  #").append(shortId(b.id())).append('\n');
        JTextArea text = new JTextArea(schedule.toString(), 18, 65); text.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(text), "Room Schedule", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel sensorPanel() {
        JPanel panel = formPanel();
        JComboBox<Room> room = new JComboBox<>(scheduler.rooms().toArray(Room[]::new));
        JTextField badge = new JTextField(25);
        JCheckBox occupied = new JCheckBox("Sensor currently detects occupancy");
        JButton report = new JButton("Send sensor data"); JButton scan = new JButton("Scan badge and check in");
        addFormRow(panel, 0, "Room", room); addFormRow(panel, 1, "Badge account ID", badge);
        GridBagConstraints oc = constraints(2); oc.gridx = 1; oc.anchor = GridBagConstraints.WEST; panel.add(occupied, oc);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT)); buttons.add(report); buttons.add(scan);
        GridBagConstraints bc = constraints(3); bc.gridx = 1; bc.anchor = GridBagConstraints.WEST; panel.add(buttons, bc);
        report.addActionListener(e -> runAction(() -> {
            scheduler.reportOccupancy(currentAccount, ((Room) room.getSelectedItem()).id(), occupied.isSelected());
            JOptionPane.showMessageDialog(this, "Occupancy reading recorded.");
        }));
        scan.addActionListener(e -> runAction(() -> {
            scheduler.sensorCheckIn(currentAccount, ((Room) room.getSelectedItem()).id(), badge.getText(), LocalDateTime.now());
            JOptionPane.showMessageDialog(this, "Badge accepted; booking checked in.");
        }));
        return panel;
    }

    private JPanel administratorPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8)); panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        DefaultTableModel model = tableModel("Account ID", "Name", "Email", "Active"); JTable table = new JTable(model);
        List<Account> rows = new ArrayList<>();
        Runnable refresh = () -> { rows.clear(); rows.addAll(scheduler.administrators()); model.setRowCount(0);
            for (Account a : rows) model.addRow(new Object[]{shortId(a.id()), a.name(), a.email(), a.active()}); };
        JButton create = new JButton("Generate administrator account"); JButton toggle = new JButton("Activate / Deactivate");
        JButton reset = new JButton("Reset password"); JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(create); actions.add(toggle); actions.add(reset);
        panel.add(new JScrollPane(table), BorderLayout.CENTER); panel.add(actions, BorderLayout.SOUTH);
        create.addActionListener(e -> createAdminDialog(refresh));
        toggle.addActionListener(e -> withSelected(table, rows, a -> runAction(() -> { scheduler.setAdministratorActive(currentAccount, a.id(), !a.active()); refresh.run(); })));
        reset.addActionListener(e -> withSelected(table, rows, a -> {
            String password = JOptionPane.showInputDialog(this,
                    "New strong password (8+ characters; uppercase, lowercase, number, symbol)");
            if (password != null) runAction(() -> scheduler.resetAdministratorPassword(currentAccount, a.id(), password));
        }));
        refresh.run(); return panel;
    }

    private void createAdminDialog(Runnable refresh) {
        JTextField name = new JTextField(20);
        JPanel form = formPanel(); addFormRow(form, 0, "Administrator name", name);
        if (JOptionPane.showConfirmDialog(this, form, "Generate Administrator Account", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
            runAction(() -> {
                GeneratedAdminCredentials credentials = scheduler.generateAdministrator(currentAccount, name.getText());
                refresh.run();
                JTextArea result = new JTextArea("Email: " + credentials.account().email()
                        + "\nTemporary password: " + credentials.temporaryPassword());
                result.setEditable(false);
                JOptionPane.showMessageDialog(this, result, "Generated Administrator Credentials",
                        JOptionPane.INFORMATION_MESSAGE);
            });
    }

    private static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout()); panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); return panel;
    }
    private static GridBagConstraints constraints(int row) {
        GridBagConstraints c = new GridBagConstraints(); c.gridy = row; c.insets = new Insets(6, 6, 6, 6); c.fill = GridBagConstraints.HORIZONTAL; return c;
    }
    private static void addFormRow(JPanel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints l = constraints(row); l.gridx = 0; l.anchor = GridBagConstraints.EAST; l.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label), l); GridBagConstraints f = constraints(row); f.gridx = 1; f.weightx = 1; panel.add(field, f);
    }
    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    }
    private static LocalDateTime parseDate(String value) {
        try { return LocalDateTime.parse(value.trim(), DATE_TIME); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("Use date/time format yyyy-MM-dd HH:mm."); }
    }
    private LocalDateTime parseDateSafe(String value) { try { return parseDate(value); } catch (RuntimeException e) { showError(e.getMessage()); return null; } }
    private static String shortId(String id) { return id.length() <= 8 ? id : id.substring(0, 8); }
    private <T> void withSelected(JTable table, List<T> rows, java.util.function.Consumer<T> action) {
        int selected = table.getSelectedRow(); if (selected < 0) { showError("Select a row first."); return; }
        action.accept(rows.get(table.convertRowIndexToModel(selected)));
    }
    private void runAction(Runnable action) {
        try { action.run(); }
        catch (RuntimeException error) { showError(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()); }
    }
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Unable to complete action", JOptionPane.ERROR_MESSAGE); }
}
