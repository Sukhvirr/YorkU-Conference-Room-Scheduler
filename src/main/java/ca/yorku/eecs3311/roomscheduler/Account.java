package ca.yorku.eecs3311.roomscheduler;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public abstract class Account {
    private final String id;
    private String name;
    private final String email;
    private String passwordHash;
    private final Role role;
    private boolean universityVerified;
    private boolean active;

    protected Account(String id, String name, String email, String passwordHash, Role role,
                      boolean universityVerified, boolean active) {
        this.id = require(id, "Account ID");
        this.name = require(name, "Name");
        this.email = require(email, "Email").toLowerCase();
        this.passwordHash = require(passwordHash, "Password");
        this.role = Objects.requireNonNull(role);
        this.universityVerified = universityVerified;
        this.active = active;
    }

    private static String require(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }

    public static String hashPassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must contain at least 6 characters.");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public boolean passwordMatches(String password) {
        return hashPassword(password).equals(passwordHash);
    }

    public String id() { return id; }
    public String name() { return name; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public Role role() { return role; }
    public boolean universityVerified() { return universityVerified; }
    public boolean active() { return active; }
    public void setName(String name) { this.name = require(name, "Name"); }
    public void setPassword(String password) { this.passwordHash = hashPassword(password); }
    public void setUniversityVerified(boolean value) { this.universityVerified = value; }
    public void setActive(boolean active) { this.active = active; }

    @Override public String toString() { return name + " (" + role.displayName() + ")"; }
}

